# BooknPlay Backend

Spring Boot API for **BooknPlay** — multi-sport venue discovery, court booking, payments, owner facility management, and admin platform tools.

## Stack

- Java 17
- Spring Boot 3.4
- MySQL
- JWT authentication
- Maven (wrapper included)

## Repo layout

| Path | Role |
|------|------|
| This repository (`booknplay`) | Backend API |
| Sibling `booknplay-frontend` | Customer, owner, and admin UIs |

## Prerequisites

- JDK 17+
- MySQL running locally (or reachable from your machine)
- Maven Wrapper (`./mvnw` / `mvnw.cmd`) — no global Maven install required

## Configuration profiles

| Profile | File | Secrets |
|---------|------|---------|
| `local` | `application-local.properties` (gitignored) | Copy from `application-local.properties.example` |
| `prod` | `application-prod.properties` | Environment variables only — see `production.env.example` |

Shared non-secret defaults live in `application.properties`. There is **no** global import of the local file; with `SPRING_PROFILES_ACTIVE=prod`, local secrets are never loaded.

## Local setup

1. Ensure MySQL is available (`createDatabaseIfNotExist=true` is set under the local profile).
2. Copy `src/main/resources/application-local.properties.example` to `src/main/resources/application-local.properties` and fill in PayHere, SMSlenz, DB, and JWT values. Do **not** commit that file.
3. Start with the `local` profile:

```bash
export SPRING_PROFILES_ACTIVE=local
./mvnw spring-boot:run
```

On Windows (cmd):

```bat
set SPRING_PROFILES_ACTIVE=local
mvnw.cmd spring-boot:run
```

Or:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

- Server: `http://localhost:8080`
- API base: `/api/v1`

## Production (VPS)

1. Create the MySQL schema and user separately (this repo does not initialize production DB).
2. Copy `production.env.example` to a secure path on the VPS (e.g. `/etc/booknplay/production.env`), set real values including `BOOKNPLAY_FRONTEND_URL` (your Vercel origin).
3. Run with `SPRING_PROFILES_ACTIVE=prod` and those env vars loaded. Hibernate uses `ddl-auto=validate`.

```bash
export SPRING_PROFILES_ACTIVE=prod
set -a && source /etc/booknplay/production.env && set +a
java -jar booknplay-*.jar
```

Do not deploy or mutate production from this README — operators own VPS setup.

## API surface

High-level route groups (all under `/api/v1`):

| Prefix | Purpose |
|--------|---------|
| `/public/*` | Venue search, details, availability |
| `/customer/*` | Customer auth, bookings, payments |
| `/owner/*` | Venues, courts, pricing, calendar, owner portal |
| `/admin/*` | Platform / homepage admin |

## Tests

```bash
./mvnw test
```

Focused run:

```bash
./mvnw -Dtest=SomeTestClass test
```

## Related

- `graphify-out/` — Graphify knowledge graph for navigating the codebase (`graphify update .` after code changes)
- `production.env.example` — required VPS environment variables
- `payhere.sandbox.env.example` / `smslenz.env.example` — optional local env helpers for the `local` profile
