# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

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
