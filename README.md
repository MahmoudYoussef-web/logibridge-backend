# LogiBridge — Delivery Integration Platform

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT_+_RBAC-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square&logo=docker&logoColor=white)
![Status](https://img.shields.io/badge/Status-Production_Ready-brightgreen?style=flat-square)

A backend platform that connects companies with delivery service providers. Companies create and track delivery orders; delivery companies receive, accept, and process them in real time.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Security Design](#security-design)
- [Error Handling](#error-handling)
- [Order Lifecycle](#order-lifecycle)
- [API Reference](#api-reference)
- [Running Locally](#running-locally)
- [Tech Stack](#tech-stack)

---

## Overview

LogiBridge solves the coordination problem between shipping companies and last-mile delivery providers. Instead of manual calls and spreadsheets, both sides operate through a single API-driven platform with full order visibility and audit trails.

**Three roles:**

| Role | Capabilities |
|---|---|
| `ROLE_COMPANY` | Register, create orders, track shipments |
| `ROLE_DELIVERY` | Accept/reject assigned orders, update status |
| `ROLE_ADMIN` | Monitor all orders, force status overrides |

---

## Architecture

```
Controller → Service → Repository → Entity
```

The system follows a layered architecture with domain logic living in the entity layer (DDD). Services orchestrate business operations; entities enforce business rules and state transitions.

```
src/main/java/com/logibridge/backend/
│
├── auth/
│   ├── controller/       # AuthController — registration, login, token management
│   ├── service/          # AuthService — register/login/logout/refresh logic
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
│   └── job/              # RefreshTokenCleanupJob — scheduled expired token cleanup
│
├── order/
│   ├── controller/       # OrderController (Company + Delivery endpoints)
│   │   └── admin/        # AdminOrderController
│   ├── service/          # OrderService, OrderQueryService, OrderAssignmentService
│   │                       OrderTrackingService
│   ├── entity/           # Order (state machine), OrderTracking (audit trail)
│   ├── validator/        # OrderValidator — ownership, access, transition checks
│   ├── specification/    # OrderSpecification — dynamic JPA filtering
│   ├── repository/       # OrderRepository, OrderTrackingRepository
│   ├── mapper/           # OrderMapper, OrderTrackingMapper
│   ├── dto/              # CreateOrderRequest, UpdateOrderStatusRequest,
│   │                       OrderResponse, OrderTrackingResponse, AdminForceStatusRequest
│   ├── enums/            # OrderStatus — with canTransitionTo() transition rules
│   ├── event/            # OrderCreatedEvent
│   ├── exception/        # InvalidOrderStateException, OrderNotFoundException,
│   │                       UnauthorizedOrderAccessException, NoDeliveryUserAvailableException
│   └── util/             # OrderNumberGenerator
│
└── common/
    └── exception/        # GlobalExceptionHandler, ApiException, ApiErrorResponse
                            ConflictException, ResourceNotFoundException, UnauthorizedException
```

---

## Security Design

### JWT — Stateless Authentication

- Access token: short-lived (15 min default), signed with HS256
- **No roles stored in the JWT** — roles are always loaded from the database on each request, preventing privilege escalation from stale or tampered tokens
- Token type claim (`ACCESS` vs `REFRESH`) validated in the filter — refresh tokens cannot be used as access tokens
- Single-parse design: token is parsed exactly once per request via `parseAndValidate()`, extracting all claims in a single operation

### Refresh Token Rotation

- Every `/refresh` call generates a new token and immediately revokes the old one
- **Reuse detection**: if a revoked token is presented again, all sessions for that user are invalidated (token theft signal)
- Tokens stored with: hash, expiry timestamp, revocation flag, revoked-at timestamp, and replacement chain (`replacedByToken`)
- Indexes on `token_hash`, `user_id`, and `expires_at` for query performance
- Scheduled cleanup job (`RefreshTokenCleanupJob`) periodically deletes expired tokens

### RBAC — Database-Driven, Server-Side Role Assignment

- Roles are **never accepted from client input** — the `role` field does not exist in `RegisterRequest`
- Separate registration endpoints enforce role at the URL level:
    - `POST /api/auth/register/company` → assigns `ROLE_COMPANY`
    - `POST /api/auth/register/delivery` → assigns `ROLE_DELIVERY`
- Admin accounts are created exclusively via server-side seeding (`RoleInitializer`)
- Method-level security with `@PreAuthorize` on every endpoint

### Rate Limiting (Anti-Brute Force)

- Max 5 failed authentication attempts per IP within a 5-minute sliding window
- Atomic check-and-record using `ConcurrentHashMap.compute()` — thread-safe without external locking
- Rate limiter is triggered **only on failures**, not on every request
- Returns `HTTP 429` with JSON error body when limit is exceeded
- Counter resets automatically on successful authentication

### Concurrency

- `@Version` optimistic locking on the `Order` entity prevents race conditions when multiple actors attempt simultaneous updates
- `OptimisticLockException` is caught at the service layer and surfaced as a retryable error message

---

## Error Handling

All errors return a consistent JSON envelope via `GlobalExceptionHandler` (`@RestControllerAdvice`):

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
| `ResourceNotFoundException` | 404 | `NOT_FOUND` | Order or user not found |
| `UnauthorizedException` | 401 | `UNAUTHORIZED` | Invalid credentials |
| `InvalidTokenException` | 401 | `INVALID_TOKEN` | Expired or reused refresh token |
| `UnauthorizedOrderAccessException` | 403 | `FORBIDDEN` | Accessing another user's order |
| `InvalidOrderStateException` | 422 | `INVALID_STATE` | Illegal status transition |
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

Transition rules are enforced inside the `OrderStatus` enum via `canTransitionTo()`. The `Order` entity exposes named methods (`accept()`, `reject()`, `markDelivered()`, etc.) — invalid transitions throw `InvalidOrderStateException` before any persistence occurs.

Every state change appends a record to `order_tracking`:

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

### Preview

<img width="1521" height="4950" alt="Swagger UI (11 04 2026 21_41)" src="https://github.com/user-attachments/assets/3f9b2125-36b8-4af6-a459-ba60d9badf59" />

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

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/orders` | Create a new delivery order |
| `GET` | `/api/orders/my` | List own orders (paginated, sortable) |
| `GET` | `/api/orders/{orderNumber}` | Get order details |
| `GET` | `/api/orders/{orderNumber}/tracking` | Full tracking history |

### Delivery — `ROLE_DELIVERY`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/delivery/orders/assigned` | List active assigned orders |
| `POST` | `/api/delivery/orders/{orderNumber}/accept` | Accept an assigned order |
| `POST` | `/api/delivery/orders/{orderNumber}/reject` | Reject an assigned order |
| `PUT` | `/api/orders/{orderNumber}/status` | Update to `IN_PROGRESS` or `DELIVERED` |

### Admin — `ROLE_ADMIN`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/admin/orders` | Filter all orders (by company, delivery, status, date range) |
| `PUT` | `/api/admin/orders/{orderNumber}/force-status` | Override status (`IN_PROGRESS`, `DELIVERED`, `CANCELLED` only) |

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

**Prerequisites:** Java 21, Maven, PostgreSQL

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

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Security | Spring Security + JWT (JJWT) |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Mapping | MapStruct |
| Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |
| Containerization | Docker + Docker Compose |

---

## Author

**Mahmoud Youssef** — Backend Engineer  
[GitHub](https://github.com/MahmoudYoussef-web)