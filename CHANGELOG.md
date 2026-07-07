# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [1.1.1] - 2026-07-07

### Fixed
- `ProdDataSourceConfig` produced a JDBC URL with port `-1` (rejected by the PostgreSQL
  driver) when `DATABASE_URL` had no explicit port, as with Render's internal database
  hostname (e.g. `postgres://user:pass@dpg-xxxx-a/dbname`). `URI.getPort()` returns `-1` in
  that case; it now falls back to PostgreSQL's default port, 5432. Added a unit test for a
  `DATABASE_URL` without an explicit port, alongside the existing one that has one.

## [1.1.0] - 2026-07-07

### Added
- Multi-stage `Dockerfile`: a Maven build stage compiles the jar, and a lightweight
  `eclipse-temurin:21-jre-alpine` runtime stage runs it as a non-root user. `JAVA_OPTS`
  is tuned for a 512 MB container (`MaxRAMPercentage`, serial GC, capped metaspace/stack).
- `render.yaml` Blueprint: a free-tier Postgres database plus a Docker-based free-plan web
  service, wired together via `fromDatabase` and health-checked at `/actuator/health`.
- A `prod` Spring profile with `server.port` read from the `PORT` environment variable and
  a `ProdDataSourceConfig` that builds the JDBC datasource from `DATABASE_URL` (Render's
  connection-string format) or, as a fallback, from separate
  `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` variables - necessary because the
  PostgreSQL JDBC driver does not accept credentials embedded in a connection URL.
- Spring Boot Actuator, with only `/actuator/health` exposed and made public (no
  authentication required), for Render's deployment health check.
- `DemoDataSeeder`: seeds a couple of clearly-fake demo customers, accounts, a `CUSTOMER`
  login and an `ADMIN` login on first boot under the `prod` profile, so the live deployment
  is immediately explorable via Swagger UI. Idempotent - safe across restarts/redeploys.
- README "Live Demo" section: the deployed URL, demo credentials, and the exact steps to
  recreate the database and reseed data once Render's free 30-day Postgres trial expires.

## [1.0.0] - 2026-07-07

### Added
- Immutable audit trail: `AuditLog` entity (JSON `details` column), recorded via a centralized
  `AuditEvent` / `AuditEventListener` mechanism for account creation, account status changes,
  transfer success/failure, and login success/failure. `AuditLogRepository` deliberately
  extends the bare `Repository` interface and exposes no update or delete operation anywhere.
- `POST /accounts` (open an account) and `PUT /accounts/{id}/status` (block/close/reactivate),
  staff-only, both auditable operations that previously had no endpoint.
- Full springdoc-openapi documentation: descriptions and request/response examples for every
  endpoint, documented error responses (400/401/403/404/409), and a `bearerAuth` JWT security
  scheme.
- GitHub Actions CI pipeline (`.github/workflows/ci.yml`): builds the project and runs the full
  test suite (including Testcontainers-based integration tests) on every push and on every pull
  request targeting `main`.
- Complete README: architecture section (double-entry bookkeeping, idempotency, optimistic
  locking, audit trail), local setup instructions, example curl requests, and a CI status badge.
- `EmailAlreadyInUseException` (409) so creating a user with a duplicate email fails clearly
  instead of with an unhandled error; a `DataIntegrityViolationException` handler as a safety
  net for the same case under concurrent requests.

### Changed
- `AccountRepository.findByCustomerId` now fetches the customer eagerly (`@EntityGraph`) to
  avoid an N+1 query.
- `AccountService.createAccount` / `updateStatus` and `UserService.createUser` / `changeRole`
  now have explicit `@Transactional` boundaries.
- Audit events are now recorded via `@TransactionalEventListener(fallbackExecution = true)`
  instead of a plain event listener: an event published from inside an already-open transaction
  is only recorded after that transaction commits (so a rolled-back operation can never produce
  a misleading audit entry), while an event published with no ongoing transaction (e.g. a
  failed login) is still recorded immediately.
- `TokenResponse` and `RefreshRequest` now mask their token fields in `toString()`, so a raw
  JWT can never leak into a log line through an accidental `toString()` call.

## [0.4.0] - 2026-07-07

### Added
- JWT authentication: `POST /auth/login` (email/password) issues a short-lived access token
  (default 15 minutes) and a longer-lived refresh token (default 7 days); `POST /auth/refresh`
  exchanges a valid refresh token for a new access token. Both lifetimes and the signing secret
  are configurable via `app.security.jwt.*` in `application.yml`.
- Role-based authorization with `CUSTOMER`, `OPERATOR` and `ADMIN` roles, enforced with
  `@PreAuthorize` at the endpoint level.
- `POST /users` and `PUT /users/{id}/role` to create users and change roles; only an ADMIN may
  assign the ADMIN role, enforced both by `@PreAuthorize` and, independently, in `UserService` -
  an OPERATOR calling either endpoint with `role=ADMIN` is rejected with 403.
- Service-layer ownership checks (`AccountService.findAccessibleById`,
  `TransactionService.transfer`): a CUSTOMER can only view or transfer from their own accounts,
  even if they know another account's id - this is checked in the service layer, not only via
  `@PreAuthorize`.
- Centralized 401 vs. 403 handling: `RestAuthenticationEntryPoint` (missing/invalid/expired
  token) and `RestAccessDeniedHandler` (authenticated but not allowed) return consistent JSON
  error bodies for both `@PreAuthorize` denials and explicit service-layer checks.
- Passwords are hashed with BCrypt and never stored, returned or logged in plain text; a failed
  login always returns the same generic message regardless of whether the email is registered,
  the password is wrong, or the account is disabled.
- `GET /accounts/{id}` endpoint to view a single account (subject to the ownership check above).
- Flyway migration `V3__users.sql` creating the `users` table.
- Integration tests covering login/token issuance, cross-customer account access (403), missing/
  invalid/expired tokens (401), the refresh flow, and an OPERATOR failing to create or promote a
  user to ADMIN.

## [0.3.0] - 2026-07-07

### Added
- `transaction` domain: double-entry bookkeeping ledger. Every transfer creates a `Transaction`
  record plus two immutable `LedgerEntry` records (one `DEBIT` on the source account, one
  `CREDIT` on the destination account); account balances are only ever changed as a side effect
  of recording these entries.
- `POST /transactions` endpoint accepting an `Idempotency-Key` header: a repeated request with
  the same key returns the original result instead of re-executing the transfer, including under
  concurrent duplicate submissions.
- Optimistic locking (`@Version`) on `Account`, with configurable retry and backoff
  (`app.transaction.retry.*`) when a transfer conflicts with another concurrent update to the
  same account.
- `InsufficientBalanceException` for transfers that exceed the source account's balance, and a
  `GlobalExceptionHandler` translating domain exceptions into clear JSON error responses.
- Flyway migration `V2__transaction_ledger.sql` adding the `version` column to `accounts` and
  creating the `transactions` and `ledger_entries` tables.
- A permissive `SecurityConfig` so the new endpoint is reachable before JWT authentication is
  implemented.
- Integration tests (Testcontainers) covering concurrent transfers on the same account, a
  repeated idempotency key, and a transfer with insufficient balance.

### Changed
- `Account` and `Customer` now use explicit constructors, getters and setters instead of
  Lombok-generated ones, so the new `@Version` optimistic-locking field is guaranteed to work
  reliably regardless of the build toolchain.

## [0.2.0] - 2026-07-06

### Added
- Initial `account`/`customer` domain model: `Customer` and `Account` JPA entities,
  `CustomerRepository` and `AccountRepository`, `CustomerService` and `AccountService`.
- `CustomerNotFoundException` / `AccountNotFoundException` (English error messages) built on
  a shared `ResourceNotFoundException` base in the `common` package.
- Flyway migration `V1__init_schema.sql` creating the `customers` and `accounts` tables
  (replaces the previous empty `V1__init.sql` placeholder).
- Unit tests for the new exceptions and services.

### Changed
- All project content (code, comments, docs, commit messages) is now written in English
  going forward, with no exceptions.

## [0.1.0] - 2026-07-06

### Added
- Initial scaffolding of the Spring Boot 3 (Java 21) project, Maven, jar packaging.
- Package-by-domain structure: `config`, `security`, `account`, `transaction`, `audit`, `common`.
- Dependencies: Spring Web, Spring Data JPA, Spring Security, Validation, PostgreSQL driver,
  Flyway, springdoc-openapi (Swagger UI), JJWT, Testcontainers, Lombok.
- `application.yml` with `dev` (local PostgreSQL) and `test` (Testcontainers) profiles.
- `docker-compose.yml` for the local PostgreSQL development environment.
- Maven Wrapper (`mvnw` / `mvnw.cmd`).
- `.gitignore` for Java/Maven/IDE projects.
- Initial README with project description and setup instructions.
