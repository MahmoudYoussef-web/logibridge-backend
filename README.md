# LogiBridge — Delivery Integration Platform

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT_+_RBAC-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Idempotency-DC382D?style=flat-square&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square&logo=docker&logoColor=white)
![Status](https://img.shields.io/badge/Status-Production_Ready-brightgreen?style=flat-square)

A production-grade backend platform that connects companies with delivery service providers. Companies create and track delivery orders; delivery companies receive, accept, and process them with guaranteed consistency — even under concurrent access and network retries.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Security Design](#security-design)
- [Idempotency System](#idempotency-system)
- [Concurrency Control](#concurrency-control)
- [Error Handling](#error-handling)
- [Order Lifecycle](#order-lifecycle)
- [API Reference](#api-reference)
- [Running Locally](#running-locally)
- [Testing](#testing)
- [Tech Stack](#tech-stack)

---

## Overview

LogiBridge solves the coordination problem between shipping companies and last-mile delivery providers. Instead of manual calls and spreadsheets, both sides operate through a single API-driven platform with full order visibility, audit trails, and production-level reliability guarantees.

**Three roles:**

| Role | Capabilities |
|---|---|
| `ROLE_COMPANY` | Register, create orders, cancel orders, track shipments |
| `ROLE_DELIVERY` | Accept/reject assigned orders, update order status |
| `ROLE_ADMIN` | Monitor all orders, force status overrides |

---

## Architecture

```
Controller → Service → Repository → Entity
```

The system follows a layered architecture with domain logic living inside the entity layer (DDD). Services orchestrate business operations; entities enforce state transitions and business rules. The idempotency and concurrency layers sit between the controller and service, protecting the domain from unsafe duplicate or concurrent mutations.

```
src/main/java/com/logibridge/backend/
│
├── auth/
│   ├── controller/       # AuthController — registration, login, token management
│   ├── service/          # AuthService — register/login/logout/refresh
│   ├── entity/           # User, Role, UserRole, RefreshToken
│   ├── dto/              # RegisterRequest, LoginRequest, AuthResponse
│   ├── repository/       # UserRepository, RoleRepository, RefreshTokenRepository
│   ├── mapper/           # AuthMapper
│   ├── enums/            # RoleName, UserStatus
│   ├── exception/        # InvalidTokenException, TokenExpiredException
│   └── config/           # RoleInitializer — seeds roles on startup
│
├── security/
│   ├── config/           # SecurityConfig, CustomUserPrincipal
│   ├── jwt/              # JwtService, JwtAuthenticationFilter, TokenType
│   │                       RestAuthenticationEntryPoint, RestAccessDeniedHandler
│   ├── service/          # CustomUserDetails, CustomUserDetailsService
│   │                       RateLimiterService, RefreshTokenService
│   └── job/              # RefreshTokenCleanupJob — scheduled token cleanup
│
├── common/
│   ├── idempotency/      # IdempotencyKey (entity), IdempotencyKeyRepository
│   │                       IdempotencyService (DB-backed)
│   │                       RedisIdempotencyService (Redis-backed)
│   │                       RedisIdempotencyConfig
│   └── exception/        # GlobalExceptionHandler, ApiException, ApiErrorResponse
│                           ConflictException, ResourceNotFoundException,
│                           UnauthorizedException, DuplicateRequestException
│
└── order/
    ├── ├── controller/       # OrderController (Company endpoints)
    │   │                   DeliveryOrderController (Delivery endpoints)
    │   └── admin/        # AdminOrderController
    ├── service/          # OrderService, OrderQueryService, OrderAssignmentService
    │                       OrderTrackingService
    ├── entity/           # Order (state machine + @Version), OrderTracking (immutable)
    ├── validator/        # OrderValidator — ownership, access, null-safe checks
    ├── specification/    # OrderSpecification — null-safe dynamic JPA filtering
    ├── repository/       # OrderRepository (with PESSIMISTIC_WRITE lock query)
    │                       OrderTrackingRepository
    ├── mapper/           # OrderMapper, OrderTrackingMapper (MapStruct)
    ├── dto/              # CreateOrderRequest, UpdateOrderStatusRequest,
    │                       OrderResponse, OrderTrackingResponse, AdminForceStatusRequest
    ├── enums/            # OrderStatus — canTransitionTo() transition rules
    ├── event/            # OrderCreatedEvent
    ├── exception/        # InvalidOrderStateException, OrderNotFoundException,
    │                       UnauthorizedOrderAccessException, NoDeliveryUserAvailableException
    └── util/             # OrderNumberGenerator
```

---

## Security Design

### JWT — Stateless Authentication

- Access token: short-lived (15 min default), signed with HS256
- **No roles stored in the JWT** — roles are always loaded from the database on each request, preventing privilege escalation from stale or tampered tokens
- Token type claim (`ACCESS` vs `REFRESH`) validated in the filter — refresh tokens cannot be used as access tokens
- Single-parse design: the token is parsed exactly once per request via `parseAndValidate()`, extracting subject, type, and expiry in a single operation

### Refresh Token Rotation

- Every `/refresh` call generates a new token and immediately revokes the old one
- **Reuse detection**: if a revoked token is presented again, all sessions for that user are invalidated (theft signal)
- Tokens stored with: hash, expiry timestamp, revocation flag, revoked-at timestamp, and a `replacedByToken` chain for forensic tracing
- Indexes on `token_hash`, `user_id`, and `expires_at` for query performance
- Scheduled `RefreshTokenCleanupJob` periodically deletes expired tokens to prevent table bloat

### RBAC — Database-Driven, Server-Side Role Assignment

- Roles are **never accepted from client input** — the `role` field does not exist in `RegisterRequest`
- Role is determined exclusively by the endpoint the client calls:
    - `POST /api/auth/register/company` → assigns `ROLE_COMPANY`
    - `POST /api/auth/register/delivery` → assigns `ROLE_DELIVERY`
- Admin accounts are created exclusively via server-side seeding (`RoleInitializer`)
- Method-level security enforced with `@PreAuthorize` on every protected endpoint

### Rate Limiting (Anti-Brute Force)

- Max 5 failed authentication attempts per IP within a 5-minute sliding window
- Atomic check-and-record using `ConcurrentHashMap.compute()` — thread-safe without external locking
- Rate limiter triggers **only on failures**, not on every request — successful auth resets the counter
- Returns `HTTP 429` with a JSON error body when the limit is exceeded

---

## Idempotency System

Network retries are a fact of production life. If a delivery agent's mobile app retries an `accept` request after a timeout, the system must not accept the order twice. LogiBridge solves this at the service layer with a dual-layer idempotency implementation.

### How It Works

Clients send a unique `Idempotency-Key` header with every state-mutating request:

```
POST /api/delivery/orders/ORD-20250115-A3B2C1/accept
Authorization: Bearer <token>
Idempotency-Key: 7f3d9a12-4b8e-4c21-9f1a-3e5b7d2c8f04
```

#### DB-Backed Layer (`IdempotencyService`)

On first request:
1. A record is inserted into `idempotency_keys` with `(key, userId)` as a unique constraint
2. The business action executes
3. The serialized response is stored in the record

On retry with the same key:
1. The existing record is found
2. The stored response is deserialized and returned immediately — **no side effects**

Race condition on simultaneous first requests:
- If two threads race on the same key, the `DataIntegrityViolationException` from the DB constraint is caught
- The losing thread fetches and returns the stored response instead of executing the action

#### Redis-Backed Layer (`RedisIdempotencyService`)

For high-performance scenarios, Redis provides sub-millisecond deduplication:

1. `SET key IN_PROGRESS NX EX 300` — atomic acquire using `setIfAbsent`
2. If already `IN_PROGRESS` → `DuplicateRequestException` (request still in flight)
3. If completed value exists → deserialize and return immediately
4. On success → overwrite key with serialized response + TTL
5. On failure → key is deleted, allowing a clean retry

```
Key format: idempotency:{userId}:{clientKey}
TTL: 5 minutes
```

### Idempotent Endpoints

| Method | Endpoint | Header Required |
|---|---|---|
| `POST` | `/api/delivery/orders/{orderNumber}/accept` | `Idempotency-Key` |
| `POST` | `/api/delivery/orders/{orderNumber}/reject` | `Idempotency-Key` |
| `POST` | `/api/orders/{orderNumber}/cancel` | `Idempotency-Key` |

---

## Concurrency Control

Multiple delivery agents or company users may attempt to modify the same order simultaneously. LogiBridge uses two complementary strategies.

### Pessimistic Locking — Mutual Exclusion

Critical write operations acquire a `PESSIMISTIC_WRITE` lock on the order row before any state change:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber")
Optional<Order> findByOrderNumberForUpdate(@Param("orderNumber") String orderNumber);
```

This is applied in `acceptOrder`, `rejectOrder`, `cancelOrder`, `updateOrderStatus`, and `adminForceUpdateStatus`. Only one transaction can hold the lock at a time — concurrent requests wait rather than racing.

### Optimistic Locking — `@Version` on Order Entity

The `Order` entity carries a `@Version` field. If two transactions read the same version and both attempt to flush, the second throws an `OptimisticLockException`, which is caught at the service layer and surfaced as a retryable `InvalidOrderStateException`.

### State Machine — Domain-Level Guard

Even if a concurrent request bypasses the lock (e.g., via a different code path), the `OrderStatus.canTransitionTo()` enum method rejects invalid transitions before any persistence occurs. This is the last line of defense — independent of locking strategy.

```
PENDING → ASSIGNED → ACCEPTED → IN_PROGRESS → DELIVERED
                   ↘ REJECTED
         ↘ CANCELLED (from PENDING, ACCEPTED, or IN_PROGRESS)
```

---

## Error Handling

All errors return a consistent JSON envelope via `GlobalExceptionHandler` (`@RestControllerAdvice`). Every exception in the system extends `ApiException`, which carries an HTTP status and a machine-readable error code — the handler has a single entry point for all business exceptions.

```json
{
  "success": false,
  "status": 409,
  "message": "Email already in use",
  "errorCode": "CONFLICT",
  "path": "/api/auth/register/company",
  "timestamp": "2025-01-15T10:30:00Z"
}
```

For validation errors, a field-level `errors` map is included:

```json
{
  "success": false,
  "status": 400,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "path": "/api/auth/register/company",
  "timestamp": "2025-01-15T10:30:00Z",
  "errors": {
    "email": "must be a well-formed email address",
    "password": "Password must contain uppercase, lowercase, and a number"
  }
}
```

**Full exception map:**

| Exception | HTTP | Error Code | Trigger |
|---|---|---|---|
| `ConflictException` | 409 | `CONFLICT` | Duplicate email on registration |
| `DuplicateRequestException` | 409 | `DUPLICATE_REQUEST` | Idempotency key already used |
| `ResourceNotFoundException` | 404 | `NOT_FOUND` | User not found |
| `OrderNotFoundException` | 404 | `NOT_FOUND` | Order not found by order number |
| `UnauthorizedException` | 401 | `UNAUTHORIZED` | Invalid credentials |
| `InvalidTokenException` | 401 | `INVALID_TOKEN` | Expired or reused refresh token |
| `UnauthorizedOrderAccessException` | 403 | `FORBIDDEN` | Accessing another user's order |
| `InvalidOrderStateException` | 422 | `INVALID_STATE` | Illegal status transition or missing header |
| `NoDeliveryUserAvailableException` | 503 | `NO_DELIVERY_AVAILABLE` | No active delivery users for assignment |
| `AccessDeniedException` | 403 | `FORBIDDEN` | Role-based access denied |
| `DataIntegrityViolationException` | 409 | `DATA_INTEGRITY` | DB constraint violation |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` | Bean validation failure |
| `Exception` (fallback) | 500 | `INTERNAL_ERROR` | Unexpected server error |

---

## Order Lifecycle

```
PENDING → ASSIGNED → ACCEPTED → IN_PROGRESS → DELIVERED
                   ↘ REJECTED
         ↘ CANCELLED (allowed from: PENDING, ACCEPTED, IN_PROGRESS)
```

Transition rules are enforced inside the `OrderStatus` enum via `canTransitionTo()`. The `Order` entity exposes named methods (`accept()`, `reject()`, `markDelivered()`, etc.) — invalid transitions throw before any persistence occurs.

`OrderTracking` records are **immutable** — a `@PreUpdate` hook throws `UnsupportedOperationException` if any update is attempted, making the audit trail tamper-proof at the ORM level.

Every state change appends a tracking record:

| Field | Description |
|---|---|
| `previous_status` | Status before the change |
| `new_status` | Status after the change |
| `changed_by` | User ID who triggered the change |
| `location` | Optional location string at time of update |
| `timestamp` | Exact time of the transition |

---

## API Reference

Full interactive docs: `http://localhost:8080/swagger-ui/index.html`

### Authentication — `/api/auth`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/register/company` | Public | Register as a company |
| `POST` | `/register/delivery` | Public | Register as a delivery user |
| `POST` | `/login` | Public | Login, receive access + refresh tokens |
| `POST` | `/refresh` | Public | Rotate refresh token, invalidate old one |
| `POST` | `/logout` | Bearer | Revoke current session's refresh token |
| `POST` | `/logout-all` | Bearer | Revoke all sessions for authenticated user |

### Company — `ROLE_COMPANY`

| Method | Endpoint | Idempotency-Key | Description |
|---|---|---|---|
| `POST` | `/api/orders` | — | Create a new delivery order |
| `GET` | `/api/orders/my` | — | List own orders (paginated) |
| `GET` | `/api/orders/{orderNumber}` | — | Get order details |
| `GET` | `/api/orders/{orderNumber}/tracking` | — | Full tracking history (paginated) |
| `POST` | `/api/orders/{orderNumber}/cancel` | ✅ Required | Cancel an order |

### Delivery — `ROLE_DELIVERY`

| Method | Endpoint | Idempotency-Key | Description |
|---|---|---|---|
| `GET` | `/api/delivery/orders/assigned` | — | List active assigned orders |
| `POST` | `/api/delivery/orders/{orderNumber}/accept` | ✅ Required | Accept an assigned order |
| `POST` | `/api/delivery/orders/{orderNumber}/reject` | ✅ Required | Reject an assigned order |
| `PUT` | `/api/orders/{orderNumber}/status` | — | Update to `IN_PROGRESS` or `DELIVERED` |

### Admin — `ROLE_ADMIN`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/admin/orders` | Filter all orders (company, delivery, status, date range) |
| `PUT` | `/api/admin/orders/{orderNumber}/force-status` | Override status (`IN_PROGRESS`, `DELIVERED`, `CANCELLED` only) |

### Idempotency-Key Usage Example

```bash
# First attempt — executes normally
curl -X POST http://localhost:8080/api/delivery/orders/ORD-20250115-A3B2C1/accept \
  -H "Authorization: Bearer <token>" \
  -H "Idempotency-Key: 7f3d9a12-4b8e-4c21-9f1a-3e5b7d2c8f04"

# Retry with the same key — returns stored response, no side effects
curl -X POST http://localhost:8080/api/delivery/orders/ORD-20250115-A3B2C1/accept \
  -H "Authorization: Bearer <token>" \
  -H "Idempotency-Key: 7f3d9a12-4b8e-4c21-9f1a-3e5b7d2c8f04"
```

Both calls return the same `OrderResponse`. The order is accepted exactly once.

---

## Running Locally

### With Docker (recommended)

```bash
git clone https://github.com/MahmoudYoussef-web/logibridge-backend.git
cd logibridge-backend
docker-compose up --build
```

API: `http://localhost:8080`  
Swagger: `http://localhost:8080/swagger-ui/index.html`

### Without Docker

**Prerequisites:** Java 21, Maven, PostgreSQL, Redis

```bash
# 1. Create the database
createdb logibridge

# 2. Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Environment Variables

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `AUTH_JWT_SECRET` | Base64-encoded HS256 secret (min 256 bits) |
| `AUTH_JWT_EXPIRATION` | Access token TTL in milliseconds (`900000` = 15 min) |

---

## Testing

The project includes a focused unit test suite covering the three most critical areas of the system: authentication logic, idempotency guarantees, and order state machine correctness.

### Test Structure

```
src/test/java/com/logibridge/backend/
├── auth/
│   └── AuthServiceTest.java           # AuthService — registration logic
├── idempotency/
│   ├── IdempotencyServiceTest.java    # IdempotencyService — deduplication + TTL expiry
│   ├── RedisIdempotencyService.java   # Redis-backed idempotency implementation
│   └── RedisIdempotencyConfig.java    # Redis test configuration
└── order/
    ├── OrderStatusTest.java           # OrderStatus enum — transition rules
    ├── OrderValidatorTest.java        # OrderValidator — ownership + state validation
    └── OrderFlowIntegrationTest.java  # @SpringBootTest — full HTTP flow (create → accept → idempotent retry)
```

### What's Tested

**`AuthServiceTest`** — verifies `AuthService` behavior using Mockito:
- Throws `ConflictException` when registering with an email already in use
- Completes successfully and returns tokens when registering with a valid, new email

**`IdempotencyServiceTest`** — verifies `IdempotencyService` deduplication guarantees:
- Returns the stored response immediately on a duplicate key — without executing the action or calling `save()`
- Executes the action and persists the response on the first request

**`OrderStatusTest`** — verifies all `OrderStatus.canTransitionTo()` rules:
- Valid transitions: `PENDING → ASSIGNED`, `PENDING → CANCELLED`, `ASSIGNED → ACCEPTED`, `ASSIGNED → REJECTED`
- Invalid transition: `PENDING → DELIVERED`
- Terminal states: `DELIVERED`, `CANCELLED`, and `REJECTED` reject all outgoing transitions

**`OrderValidatorTest`** — verifies `OrderValidator` guard methods:
- `validateOwnership()` throws `UnauthorizedOrderAccessException` when the company ID does not own the order
- `validateOwnership()` passes silently when the correct company ID is provided
- `validateTransition()` throws `InvalidOrderStateException` on illegal state changes (e.g., `DELIVERED → IN_PROGRESS`)
- `validateTransition()` throws `InvalidOrderStateException` when the order object is `null`

### Running the Tests

```bash
./mvnw test
```

Unit tests use JUnit 5 + Mockito — no Spring context or database required. They run in isolation and complete in under a second.

`OrderFlowIntegrationTest` is a full `@SpringBootTest` integration test that boots the real application context and exercises the HTTP layer end-to-end: it registers a company and delivery user via the API, creates an order, accepts it, then retries the accept with the same `Idempotency-Key` and asserts both responses are identical. Requires a running PostgreSQL and Redis instance (Docker Compose recommended).
---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Security | Spring Security + JWT (JJWT) |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Cache / Idempotency | Redis |
| Mapping | MapStruct |
| Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |
| Containerization | Docker + Docker Compose |

---

## Author

**Mahmoud Youssef** — Backend Engineer  
[GitHub](https://github.com/MahmoudYoussef-web)