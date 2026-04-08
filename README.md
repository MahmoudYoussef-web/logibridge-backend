# 🔐 LogiBridge Authentication & RBAC System

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.x-green)
![Security](https://img.shields.io/badge/Security-JWT%20%2B%20RBAC-red)
![Status](https://img.shields.io/badge/Status-In%20Progress-yellow)

---

## 🚀 Overview

This module implements a **production-grade authentication and authorization system** for the LogiBridge platform.

It is designed with a strong focus on:

- Security-first architecture
- Stateless authentication (JWT)
- Database-driven authorization (RBAC)
- Token lifecycle management
- Protection against replay attacks

> This is not a basic JWT setup — it is designed to handle real-world security scenarios.

---

## 🧠 Architecture Principle

```

JWT → Identity (WHO)
DB  → Authorization (WHAT)

```

- JWT is used only for authentication
- Roles and permissions are always resolved from the database

---

## 🔄 Authentication Flow

### 🔑 Login

```

POST /api/auth/login

```

Flow:

1. User submits credentials
2. AuthenticationManager validates password (BCrypt)
3. User + roles loaded from DB
4. JWT access token generated
5. Refresh token stored in DB
6. Tokens returned to client

---

### 📡 Request Flow

```

Authorization: Bearer <access_token>

```

1. JwtAuthenticationFilter intercepts request
2. Token is validated (signature + expiration)
3. Token type enforced (ACCESS only)
4. User loaded from DB
5. Roles mapped to authorities
6. SecurityContext populated

---

### 🔁 Refresh Token Flow

```

POST /api/auth/refresh

```

- Refresh token validated from DB
- Old token revoked
- New access + refresh tokens generated

---

### 🚨 Reuse Detection (Critical Security)

If a revoked refresh token is reused:

→ All user sessions are invalidated  
→ All refresh tokens are deleted  

This prevents:

- Token theft reuse
- Replay attacks

---

### 🚪 Logout

- Single device logout → revoke token  
- Logout all devices → delete all refresh tokens  

---

## 🛡️ Authorization (RBAC)

### Model

```

User → UserRole → Role

```

### Why DB-driven roles?

- Prevent stale permissions
- Immediate role updates
- No trust in client-side tokens

---

## 🔐 Security Features

- JWT signature validation
- Token expiration enforcement
- Token type validation (ACCESS vs REFRESH)
- BCrypt password hashing
- Role-based authorization using `@PreAuthorize`
- No sensitive data exposure
- Centralized exception handling

---

## 🧠 Key Design Decisions

### 1. JWT بدون Roles

- Prevent privilege escalation
- Avoid stale authorization

---

### 2. Refresh Tokens في DB

- Enables logout
- Enables rotation
- Enables revocation

---

### 3. Refresh Token Rotation

- One-time use tokens
- Prevent replay attacks

---

### 4. Stateless + Stateful Hybrid

- Stateless access tokens → scalability
- Stateful refresh tokens → control

---

## ⚖️ Trade-offs

| Decision                  | Trade-off                  |
|--------------------------|----------------------------|
| DB lookup per request    | Higher latency             |
| No roles in JWT          | Requires DB access         |
| Token rotation           | More complexity            |

---

## 🧪 Edge Cases Handled

- Expired tokens
- Invalid tokens
- Reused refresh tokens
- Unauthorized access
- Disabled users
- Locked accounts

---

## 📁 Module Structure

```

auth/
security/
common/

```

---
تمام 🔥 — اللي عندك ده ممتاز جدًا كـ Auth README
بس دلوقتي لازم نعمل **Upgrade ذكي** علشان يعكس إن المشروع بقى:

> مش Auth بس → **Platform فيه Order Module**

---

# 🎯 **Problem Summary**

الـ README الحالي:

✔ قوي جدًا في الـ Auth
❌ لكنه مش بيعكس الشغل الجديد (Order Module)

---

# 🧠 **Root Cause Analysis**

أنت بنيت:

* Auth system 🔐
* Order system 🚚 (تقيل جدًا)

لكن README لسه:

> مركز على جزء واحد بس

---

# ✅ **Correct Solution**

هنعمل:

> ✨ **Extend مش Replace**

يعني:

✔ نسيب الـ Auth زي ما هو
✔ ونضيف **Order Module Section احترافي**

---

# 🚀 **انسخ الجزء ده وضيفه تحت الـ README**

## 🔥 (ده الجزء الجديد)

```md
---

# 🚚 Order & Tracking Module

## 🚀 Overview

The Order module represents the **core business engine** of LogiBridge.

It connects:

- Companies → create delivery orders
- Delivery companies → process and update orders
- Admin → monitor and control system activity

> Order = Source of truth  
> Tracking = Full history of changes

---

## 🔄 Order Lifecycle

```

PENDING → IN_PROGRESS → DELIVERED
↘
CANCELLED

````

### Rules:

- Orders start as `PENDING`
- Only delivery users can update status
- Invalid transitions are blocked inside domain logic

---

## 🧠 Core Design Concepts

### 1. Entity-driven logic

```java
order.markInProgress();
order.markDelivered();
````

✔ Prevents invalid state transitions
✔ Keeps business logic inside domain

---

### 2. Tracking System (Audit Trail)

Every status change creates a new record:

```
order_tracking
```

Fields:

* previous_status
* new_status
* location
* timestamp
* changed_by

✔ Full history
✔ Debugging friendly
✔ UI timeline ready

---

### 3. Order Number Strategy

* Human-readable (`ORD-YYYY-XXXX`)
* No database ID exposure
* More secure and professional

---

## ⚙️ Order Flow

### Create Order

1. Validate request
2. Generate order number
3. Assign delivery company
4. Save order
5. Create tracking record

---

### Update Status

1. Validate ownership (RBAC)
2. Validate transition
3. Update order
4. Save tracking entry

---

## 🛡️ Security Integration

* Fully integrated with JWT authentication
* Role-based access enforced using `@PreAuthorize`
* Ownership validation inside service layer

Example:

```java
if (!order.isAssignedToDelivery(userId)) {
    throw UnauthorizedOrderAccessException;
}
```

---

## 📡 API Overview

### Create Order

```
POST /api/orders
```

---

### Get Orders

```
GET /api/orders/my
```

---

### Update Status

```
PUT /api/orders/{orderNumber}/status
```

---

### Tracking

```
GET /api/orders/{orderNumber}/tracking
```

---

## ⚖️ Trade-offs

| Decision          | Trade-off             |
| ----------------- | --------------------- |
| Tracking table    | More storage usage    |
| Entity logic      | More complex entities |
| Simple assignment | Not optimized yet     |

---

## 🧪 Edge Cases Handled

* Invalid status transitions
* Unauthorized access
* Missing delivery assignment
* Order not found
* Concurrent updates

---

## 🎯 What This Module Demonstrates

* Business-driven design
* State machine handling
* Audit logging system
* Secure multi-role access
* Clean separation of read/write logic

````
## 🚧 Current Status

- ✅ Authentication system
- ✅ RBAC implementation
- ✅ Order management 
- ⏳ Delivery workflow
- ⏳ Admin dashboard

---

## 👨‍💻 Author

Mahmoud Youssef  
Backend Engineer (Spring Boot)

---

## 🏁 Final Result

✔ Secure  
✔ Scalable  
✔ Production-ready  
✔ Designed for real-world usage  
```
