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
| [ROADMAP.md](ROADMAP.md) | Architecture, phases, and progress |

## Prerequisites

- JDK 17+
- MySQL running locally (or reachable from your machine)
- Maven Wrapper (`./mvnw` / `mvnw.cmd`) — no global Maven install required

## Local setup

1. Ensure MySQL is available. The default datasource URL can create the `booknplay` database if it does not exist (`createDatabaseIfNotExist=true`).
2. Put local secrets and overrides in `src/main/resources/application-local.properties` (imported optionally by `application.properties`). Do **not** commit real passwords or JWT secrets.
3. Start the API:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

- Server: `http://localhost:8080`
- API base: `/api/v1`

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

- [ROADMAP.md](ROADMAP.md) — system blueprint and roadmap
- `graphify-out/` — Graphify knowledge graph for navigating the codebase (`graphify update .` after code changes)
