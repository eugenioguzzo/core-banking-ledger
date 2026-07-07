# core-banking-ledger

[![CI](https://github.com/eugenioguzzo/core-banking-ledger/actions/workflows/ci.yml/badge.svg)](https://github.com/eugenioguzzo/core-banking-ledger/actions/workflows/ci.yml)

A core banking ledger service built with Spring Boot 3 and Java 21. It models customers,
accounts and money transfers as a **double-entry bookkeeping ledger**, exposed through a REST
API secured with JWT authentication and role-based authorization, with every key operation
recorded in an immutable audit trail.

This project is a portfolio piece: it is not connected to a real payment network or bank, but
its domain logic (ledger integrity, idempotency, concurrency control, authorization boundaries
and auditability) is built to the same standards a real banking backend would need.

## Live Demo

The app is deployed on [Render](https://render.com) using the `Dockerfile` and `render.yaml`
Blueprint in this repository.

> Update this link once you deploy your own instance - Render assigns
> `https://<service-name>.onrender.com` by default.

- **App**: https://core-banking-ledger.onrender.com
- **Swagger UI**: https://core-banking-ledger.onrender.com/swagger-ui.html

The free instance spins down after a period of inactivity, so the first request after a while
may take up to a minute while it wakes back up - just retry it.

### Demo credentials

On first boot, `DemoDataSeeder` (active only under the `prod` profile) seeds a couple of
clearly-fake demo customers, accounts and users, so the API is explorable right away with no
setup:

| Role       | Email                    | Password       | Notes                                                          |
|------------|--------------------------|----------------|------------------------------------------------------------------|
| `CUSTOMER` | `alice.demo@example.com` | `DemoPass123!` | Owns an account with a 1000.00 EUR balance                       |
| `ADMIN`    | `admin.demo@example.com` | `DemoPass123!` | Can open accounts, change account status, create/promote users   |

A second demo customer, "Bob Demo", has a 500.00 EUR account with no login of its own - a
handy destination account when trying `POST /transactions` as Alice.

These accounts are fake, seeded purely for this demo - don't reuse this password anywhere else.

### The free Postgres database expires after 30 days

Render deletes free-tier PostgreSQL databases 30 days after creation. When that happens, the
web service's health check starts failing because it can no longer reach the database. To fix
it:

1. In the Render dashboard, create a new free PostgreSQL database (delete the old, expired one
   if it's still listed) - or, if you manage this deployment via the `render.yaml` Blueprint,
   just re-sync the Blueprint and Render recreates it for you.
2. Open the web service's **Environment** tab and confirm `DATABASE_URL` points at the new
   database's connection string (re-syncing the Blueprint updates this automatically, since
   it's wired via `fromDatabase`; if you set it up by hand, paste in the new connection string).
3. Trigger a manual deploy (or wait for the next automatic one).

That's the whole procedure - no manual reseeding step. On that startup, Flyway rebuilds the
schema from scratch against the new, empty database (`src/main/resources/db/migration`), and
`DemoDataSeeder` runs right after and reseeds the demo customers, accounts and users.

## Tech stack

- Java 21
- Spring Boot 3 (Web, Data JPA, Security, Validation)
- PostgreSQL + Flyway (schema migrations)
- JWT (JJWT) authentication with BCrypt password hashing
- springdoc-openapi (Swagger UI)
- Testcontainers (integration tests against a real PostgreSQL instance)
- GitHub Actions (CI)
- Docker, deployed to Render (CD)
- Maven

## Package structure

```
com.eugeniokg.corebankingledger
├── config       # application configuration (OpenAPI, etc.)
├── security     # authentication, authorization, JWT, users
├── account      # customer and account domain
├── transaction  # double-entry ledger and transfers
├── audit        # immutable audit trail
└── common       # shared building blocks (base exceptions, error responses, utilities)
```

## Architecture

### Double-entry bookkeeping

An account's balance is never written to directly by application logic. Every transfer
(`POST /transactions`) creates one `Transaction` record plus exactly two immutable
`LedgerEntry` records: a `DEBIT` on the source account and a matching `CREDIT` on the
destination account, both for the same amount. The account's `balance` column is a cache that
is only ever updated as a side effect of recording these entries, inside the same database
transaction - so the balance can always be independently reconstructed and verified from the
ledger entries, and a partial write (entries without a balance update, or vice versa) is not
possible. See `LedgerTransferExecutor`.

### Idempotency

Every transfer request carries a client-generated `Idempotency-Key` header. The first request
with a given key executes the transfer and stores the key on the resulting `Transaction`
(a unique database column); any later request with the same key - including a genuine
concurrent duplicate submission - returns the original result instead of moving money twice.
See `TransactionService`.

### Optimistic locking for concurrency control

`Account` carries a JPA `@Version` column. Two transfers that touch the same account
concurrently will cause one of them to fail with an optimistic locking conflict at commit time
rather than silently overwriting the other's balance update (a "lost update"). `TransactionService`
catches this conflict and retries the whole transfer attempt (re-reading fresh account state)
with a configurable exponential backoff, so the caller sees a successful response without
needing to implement retry logic itself. See `app.transaction.retry.*` in `application.yml`.

### Audit trail

Key operations - account creation, account status changes (block/close/reactivate), transfers
(both completed and failed), and login attempts (both successful and failed) - publish an
`AuditEvent` that a single centralized listener (`AuditEventListener`) turns into an `AuditLog`
row. The audit trail is append-only: `AuditLogRepository` deliberately extends the bare
`Repository` marker interface (not `JpaRepository`) and declares only `save` and read methods -
no update or delete operation exists anywhere in the codebase for this entity. An event
published from inside an already-open transaction (e.g. account creation) is only recorded
after that transaction commits, so a rolled-back operation never leaves a misleading audit
entry; an event published for an operation that has no wrapping transaction (e.g. a failed
login) is recorded immediately, in its own independent transaction, so it is never lost even
though the triggering operation itself did not persist anything.

### Authentication and authorization

- `POST /auth/login` returns a short-lived access token (default 15 minutes) and a longer-lived
  refresh token (default 7 days); `POST /auth/refresh` exchanges a valid refresh token for a new
  access token. Both lifetimes and the signing secret are configurable via `app.security.jwt.*`.
  Access and refresh tokens carry a `type` claim and are never interchangeable.
- Three roles: `CUSTOMER` (can only view or transfer from their own accounts), `OPERATOR` (can
  manage accounts and users, but can never create or promote a user to `ADMIN`), and `ADMIN`
  (full access).
- Authorization is checked twice: with `@PreAuthorize` at the endpoint, and again in the service
  layer (`AccountService`, `TransactionService`, `UserService`) - so a customer can never reach
  another customer's account, and an operator can never mint an admin, even by calling the
  service layer in a way that bypasses the controller's own check.
- Passwords are hashed with BCrypt and never stored, returned, or logged in plain text. A failed
  login always returns the same generic error, whether the email is unknown, the password is
  wrong, or the account is disabled.
- A missing, invalid or expired token yields `401 Unauthorized`; an authenticated request that
  isn't allowed yields `403 Forbidden` - both handled centrally and consistently, whether the
  denial comes from `@PreAuthorize` or from an explicit check in a service.

## Local setup

### Prerequisites

- JDK 21
- Docker (for local PostgreSQL and for the Testcontainers-based integration tests)
- Maven (or the `mvnw` / `mvnw.cmd` wrapper included in the project)

### Start the local database

```bash
docker compose up -d
```

Starts a PostgreSQL container on `localhost:5432` with database `core_banking_ledger`, user
`ledger`, password `ledger`.

### Run the application

```bash
mvn spring-boot:run
```

The `dev` profile is active by default and connects to the PostgreSQL instance started above.
The API is available at `http://localhost:8080`; interactive API documentation (Swagger UI) is
at `http://localhost:8080/swagger-ui.html`.

### Run the tests

```bash
mvn test
```

This runs both plain unit tests and the Testcontainers-based integration tests (which start
their own ephemeral PostgreSQL container, independent of the one from `docker compose`), so
Docker must be running. The same command is what CI runs on every push and on every pull
request targeting `main` (see `.github/workflows/ci.yml`).

## Example requests

Every example below assumes the app is running locally on port 8080. Replace ids and tokens
with real values returned by the previous call.

**Log in and obtain tokens:**

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "jane.doe@example.com", "password": "correct-horse-battery-staple"}'
```

**Refresh an access token:**

```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<refreshToken from login>"}'
```

**Open a new account for a customer (staff only - OPERATOR or ADMIN token):**

```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6", "currency": "EUR"}'
```

**Transfer funds between two accounts:**

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -H "Idempotency-Key: 5f8d3c2a-1b4e-4f6a-9c3d-2e1f4a5b6c7d" \
  -d '{"sourceAccountId": "<your account id>", "destinationAccountId": "<destination account id>", "amount": 100.00, "description": "Rent payment"}'
```

## Application profiles

| Profile | Description                                                          |
|---------|-------------------------------------------------------------------------|
| `dev`   | Local PostgreSQL (Docker Compose), for development                       |
| `test`  | PostgreSQL started dynamically via Testcontainers                        |
| `prod`  | Reads connection details from environment variables; used by the Docker/Render deployment |

### Building and running the Docker image locally

```bash
docker build -t core-banking-ledger .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET=a-local-test-secret-at-least-32-bytes-long \
  -e DATABASE_URL=postgres://ledger:ledger@host.docker.internal:5432/core_banking_ledger \
  core-banking-ledger
```

This targets the `dev` Postgres instance started by `docker compose up -d` from the host
machine (`host.docker.internal` resolves to the host from inside the container). `prod` builds
its datasource from `DATABASE_URL` at startup - see `ProdDataSourceConfig`.

## Roadmap

- [x] Domain model for `account`/`customer` (entities, repositories, not-found exceptions)
- [x] Domain model for `transaction` (double-entry ledger, idempotency, optimistic locking)
- [x] `POST /transactions` endpoint
- [x] JWT authentication and role-based authorization
- [x] Immutable audit trail
- [x] OpenAPI documentation with request/response examples and documented error responses
- [x] CI pipeline
- [x] Docker image and Render deployment (Blueprint, health check, demo data)
- [ ] REST endpoints for full customer management (beyond account creation/status/lookup)
