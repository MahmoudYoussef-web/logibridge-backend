# 🚚 LogiBridge - Delivery Integration Platform

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.x-green)
![Security](https://img.shields.io/badge/Security-JWT%20%2B%20RBAC-red)
![Status](https://img.shields.io/badge/Status-Production%20Ready-brightgreen)

---

# 🚀 Overview

LogiBridge is a **production-grade delivery integration platform** that connects:

* 🏢 Companies → create and track delivery orders
* 🚚 Delivery companies → accept and process orders
* 🛡️ Admin → monitor and control system

> Designed using real-world backend engineering practices: security, scalability, and clean architecture.

---

# 🧠 Architecture

```text
Controller → Service → Repository → Entity
```

### Core Principles:

* JWT → Authentication (WHO)
* DB → Authorization (WHAT)
* Entity → Business Logic (HOW)

---

# 🏗️ Project Structure

```text
src/main/java/com/logibridge/backend/

├── auth         → Authentication & user management
├── security     → JWT + filters + rate limiting
├── order        → Core business module
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   ├── mapper
│   ├── validator
│   ├── specification
├── common       → Shared utilities & exceptions
```

---

# 🔐 Authentication & Security

### ✔ Features:

* JWT Authentication (stateless)
* Refresh Token Rotation (stored in DB)
* RBAC (database-driven roles)
* BCrypt password hashing
* Token type validation (ACCESS vs REFRESH)
* Centralized exception handling

---

# 🛡️ Advanced Security

## 🔥 Audit Logging

Tracks all critical actions:

* AUTH_SUCCESS / AUTH_FAILED
* ORDER_CREATED / UPDATED / CANCELLED
* DELIVERY_ACCEPT / REJECT
* ADMIN_FORCE_STATUS

Example:

```
[AUDIT] action=ORDER_ACCEPTED userId=5 role=ROLE_DELIVERY
```

---

## 🔥 Rate Limiting (Anti-Brute Force)

* Max 5 failed attempts per IP
* 5-minute window
* Returns HTTP 429 when exceeded
* Thread-safe implementation (ConcurrentHashMap)

---

## 🔥 Secure JWT Design

* No roles inside JWT
* Roles always loaded from DB
* Prevents privilege escalation

---

# 📦 Order & Tracking System

## 🔄 Order Lifecycle

```text
PENDING → ASSIGNED → ACCEPTED → IN_PROGRESS → DELIVERED
↘
REJECTED / CANCELLED
```

---

## 🧠 Domain-Driven Design

Business logic inside entity:

```java
order.accept();
order.reject();
order.markDelivered();
```

✔ Prevents invalid transitions
✔ Keeps logic centralized

---

## 📊 Tracking System (Audit Trail)

Every state change creates a tracking record:

* previous_status
* new_status
* changed_by
* timestamp
* location

---

# ⚙️ Core Services

## OrderService

Handles:

* Create Order
* Accept / Reject
* Update Status
* Cancel Order
* Admin Force Update

✔ Includes validation + concurrency handling + audit logging

---

## OrderQueryService

* Filtering
* Company orders
* Delivery orders

✔ Uses Specification pattern

---

## OrderAssignmentService

* Assigns delivery users randomly (MVP)
* Based on ROLE_DELIVERY users

---

# 👨‍💼 Admin Module

### Features:

* Filter all orders
* Force order status

### Rules:

Admin can force only:

* IN_PROGRESS
* DELIVERED
* CANCELLED

---

# 🛡️ Validation Layer

OrderValidator:

* validateOwnership
* validateDeliveryAccess
* validateTransition

---

# ⚡ Concurrency Handling

* Uses Optimistic Locking (@Version)
* Prevents race conditions

---

# 🌐 API Endpoints

## 🔐 Authentication

POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout

---

## 🏢 Company

POST /api/orders
GET /api/orders/my
GET /api/orders/{orderNumber}
GET /api/orders/{orderNumber}/tracking

---

## 🚚 Delivery

GET /api/delivery/orders/assigned
POST /api/delivery/orders/{orderNumber}/accept
POST /api/delivery/orders/{orderNumber}/reject
PUT /api/orders/{orderNumber}/status

---

## 🛡️ Admin

GET /api/admin/orders
PUT /api/admin/orders/{orderNumber}/force-status

---

# 🔄 Order Status Values

PENDING
ASSIGNED
ACCEPTED
IN_PROGRESS
DELIVERED
REJECTED
CANCELLED

---

# 📡 API Documentation

```
http://localhost:8080/swagger-ui/index.html
```

---

# 🧪 Edge Cases Handled

* Invalid state transitions
* Unauthorized access
* Expired tokens
* Reused refresh tokens
* Concurrent updates
* Rate limit exceeded

---

# 🧰 Tech Stack

| Category   | Technology            |
| ---------- | --------------------- |
| Language   | Java 21               |
| Framework  | Spring Boot           |
| Security   | Spring Security + JWT |
| ORM        | Spring Data JPA       |
| Database   | PostgreSQL            |
| Build Tool | Maven                 |
| Docs       | Swagger               |

---

# 🚀 How to Run

```bash
git clone <repo>
cd logibridge-backend
mvn clean install
mvn spring-boot:run
```

---

# 🔐 Configuration

Create:

```
application.properties
```

Example:

```
auth.jwt.secret=your-secret
auth.jwt.expiration=900000
```

---

# 🎯 Highlights (For Interviews)

* Designed **JWT + Refresh Token Rotation system**
* Built **RBAC with DB-driven roles**
* Implemented **order lifecycle state machine**
* Added **audit logging system**
* Implemented **rate limiting for security**
* Applied **DDD principles in entity layer**
* Handled **concurrency with optimistic locking**

---

# 👨‍💻 Author

Mahmoud Youssef
Backend Engineer (Spring Boot)

---

# 🏁 Final Result

✔ Secure
✔ Scalable
✔ Clean architecture
✔ Production-ready
✔ Real-world backend system

---
