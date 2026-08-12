# User Service — Documentation

User **profile / directory** microservice for the Chatter platform. It owns
everything a "person" is beyond their login credentials — display name, avatar,
bio, status — plus user search.

**Tech stack:** Java 25, Spring Boot 4.1.0, PostgreSQL 16, Flyway,
Spring Cloud (Eureka), Spring Data JPA.

---

## Table of Contents

1. [Project Structure](#1-project-structure)
2. [Why a separate service?](#2-why-a-separate-service)
3. [Domain Model](#3-domain-model)
4. [Endpoints](#4-endpoints)
5. [Internal API & Guarding](#5-internal-api--guarding)
6. [Service Layer](#6-service-layer)
7. [Database](#7-database)
8. [How to Run](#8-how-to-run)

---

## 1. Project Structure

```
src/main/java/com/prashant/user_service/
├── UserServiceApplication.java        # Entry point
├── config/
│   └── InternalApiKeyFilter.java      # Protects /internal/** (service-to-service)
├── controller/
│   ├── UserProfileController.java     # Public (gateway-protected) directory API
│   └── InternalUserController.java    # Inbound call from auth-service
├── service/
│   └── UserProfileService.java        # Profile CRUD + search logic
├── repository/
│   └── UserProfileRepository.java
├── entity/
│   ├── UserProfile.java              # The directory entity
│   └── UserStatus.java               # ONLINE / AWAY / BUSY / OFFLINE
├── dto/
│   ├── CreateUserProfileRequest.java
│   ├── UpdateUserProfileRequest.java
│   └── UserProfileDto.java
└── exception/
    ├── UserNotFoundException.java
    └── GlobalExceptionHandler.java
```

---

## 2. Why a separate service?

Google Chat treats **identity** (who can log in) and **directory** (who is in
your org / how they look) as different concerns:

| Concern | Owner |
|---------|-------|
| Passwords, lock state, tokens, roles/permissions | `auth-service` |
| Display name, avatar, bio, status, search, profile updates | `user-service` |

This gives the platform independent scaling, release cycles and data stores.
The two services agree on a **user id**: the profile row uses the identity's
UUID as its primary key.

---

## 3. Domain Model

### `entity/UserProfile.java`

```
user_profiles
├── id          UUID     PK (== auth-service user id)
├── username    VARCHAR(50)  UNIQUE
├── display_name VARCHAR(100) NOT NULL
├── email       VARCHAR(100) UNIQUE
├── avatar_url  VARCHAR(500)
├── bio         VARCHAR(500)
├── status      VARCHAR(20)  'OFFLINE' (EnumType.STRING)
├── created_at  TIMESTAMP  DEFAULT now()
└── updated_at  TIMESTAMP
```

Status is mapped with `@Enumerated(EnumType.STRING)` (readable values in DB)
and `@Builder.Default` so the builder default survives Lombok.

---

## 4. Endpoints

All served behind the gateway at `/user-service/**` (rewritten to
`/api/users/**`). The gateway **already validated the JWT** and forwards the
caller identity:

| Method | Path | Reads | Description |
|--------|------|-------|-------------|
| GET | `/api/users/me` | `X-User-Id` | Current user's profile |
| GET | `/api/users/{id}` | path | Profile of any user |
| GET | `/api/users/search?q=` | query | Search by display name / username |
| PATCH | `/api/users/me` | `X-User-Id` | Update display name, avatar, bio, status |

```java
@GetMapping("/me")
public ResponseEntity<UserProfileDto> me(@RequestHeader("X-User-Id") UUID userId) {
    return ResponseEntity.ok(userProfileService.getProfile(userId));
}
```

> **Trust model:** `X-User-Id` is injected by the gateway after cryptographic
> JWT validation. Services inside the network trust those headers; external
> callers can only reach them through the gateway.

---

## 5. Internal API & Guarding

`POST /internal/users` is **not** exposed through the gateway. The
auth-service calls it directly (load-balanced `http://user-service`).

### `config/InternalApiKeyFilter.java`

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/internal/");
}

@Override
protected void doFilterInternal(...) {
    String presentedKey = request.getHeader("X-Internal-Key");
    if (presentedKey == null || !presentedKey.equals(internalApiKey)) {
        // 403 + JSON error
        return;
    }
    filterChain.doFilter(request, response);
}
```

| Part | Purpose |
|------|---------|
| `shouldNotFilter` | Only `/internal/**` paths are checked — public/API traffic unaffected. |
| `internal.api-key` | Configured value (default `dev-internal-key`, override with `INTERNAL_API_KEY`). |
| 403 on mismatch | Internal endpoints are unreachable by callers who don't know the shared key. |

Uniqueness conflicts are handled by making creation **idempotent by username**:

- re-sending the same `username` returns the existing profile instead of failing
- a duplicate `email` (different username) is rejected explicitly

This makes the auth-service register call safe to retry.

---

## 6. Service Layer

### `service/UserProfileService.java`

```java
@Transactional
public UserProfileDto createProfile(CreateUserProfileRequest request) {
    if (userProfileRepository.existsByUsername(request.username())) {   // idempotent
        return mapToDto(userProfileRepository.findByUsername(request.username()).orElseThrow());
    }
    if (userProfileRepository.existsByEmail(request.email())) {
        throw new IllegalArgumentException("Email already registered: " + request.email());
    }
    UserProfile profile = UserProfile.builder()
            .id(request.id())            // identity user id from auth-service
            .username(request.username())
            .displayName(request.displayName())
            .email(request.email())
            .build();
    return mapToDto(userProfileRepository.save(profile));
}
```

| Method | Behavior |
|--------|----------|
| `createProfile` | Upsert-by-username: idempotent, throws on duplicate email. |
| `getProfile(id)` | Throws `UserNotFoundException` → 404. |
| `searchProfiles(q)` | `Containing`-match on display name **or** username (case-insensitive). |
| `updateProfile(id, req)` | Non-null fields only; status via `UserStatus`. |
| `updateStatus(id, status)` | Dedicated primitive for future presence updates. |

---

## 7. Database

Flyway migration `V1__create_user_profiles.sql`:

```sql
CREATE TABLE IF NOT EXISTS user_profiles (
    id           UUID PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    email        VARCHAR(100) NOT NULL UNIQUE,
    avatar_url   VARCHAR(500),
    bio          VARCHAR(500),
    status       VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE',
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now()
);
```

`ddl-auto: none` — Flyway owns the schema (`baseline-on-migrate: true`).

---

## 8. How to Run

```bash
# create the database once
docker run -d --name user-postgres \
  -e POSTGRES_DB=user_profile_db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:16-alpine

# start Eureka first, then:
.\gradlew.bat bootRun
```

The service runs on a random port (`server.port: 0`) and registers with Eureka.
Test end-to-end through the gateway:

```bash
curl http://localhost:8080/user-service/users/me -H "Authorization: Bearer <jwt>"

curl "http://localhost:8080/user-service/users/search?q=ali" -H "Authorization: Bearer <jwt>"
```