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

## 🎯 What This Module Demonstrates

- Secure authentication design
- RBAC implementation
- Token lifecycle management
- Defensive security practices
- Production-ready architecture

---

## 🚧 Current Status

- ✅ Authentication system
- ✅ RBAC implementation
- ⏳ Order management (coming next)
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
