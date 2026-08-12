# User Service

User directory & profile microservice for the Chatter platform.

## Tech Stack

- Spring Boot 4.1
- Java 25
- PostgreSQL 16+
- Flyway
- Eureka client
- Load-balanced service-to-service calls (inbound via discovery)

## Responsibility

`user-service` owns the **user profile** (directory) domain:

| Endpoint (via gateway `/user-service/**`) | Auth | Description |
|------------------------------------------|------|-------------|
| GET  `/api/users/me`        | Yes | Current user's profile |
| GET  `/api/users/{id}`      | Yes | Profile by user id |
| GET  `/api/users/search?q=` | Yes | Search users by name/username |
| PATCH `/api/users/me`       | Yes | Update display name, avatar, bio, status |

Internal endpoint (NOT routed through the gateway; service-to-service only):

| Method | Path          | Guard               | Description |
|--------|---------------|---------------------|-------------|
| POST   | `/internal/users` | `X-Internal-Key` | Create a profile (called by auth-service during registration) |

The profile `id` is the **same user id** as the identity account created by the auth-service.

## Setup

### 1. Create the database

```bash
docker run -d \
  --name user-postgres \
  -e POSTGRES_DB=user_profile_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. Run

```bash
./gradlew bootRun
```

The service runs on a random port (`server.port: 0`) and registers with Eureka.
The gateway resolves it via `lb://user-service`.

## Integration with auth-service

- During registration, auth-service calls `POST http://user-service/internal/users`
  with the identity's user id, username and email.
- The call is protected by a shared `X-Internal-Key` header (`internal.api-key`,
  configured in both services, overridable via `INTERNAL_API_KEY`).
- The gateway routes public requests from `/user-service/**` to `/api/users/**`.

## Running the platform

The auth-service and user-service must be pointed at the same Postgres instance,
Redis must be running for auth, and Eureka must be up. See the root README for
the full `docker-compose` quickstart.