# core-banking-ledger

Scaffolding for a core banking ledger service, built with Spring Boot 3 and Java 21.
The project will expose REST APIs for managing accounts, transactions and audit trails,
with JWT-based authentication and PostgreSQL persistence.

> Current status: `account`/`customer` domain model plus a double-entry `transaction` ledger
> with idempotent, concurrency-safe transfers via `POST /transactions`. JWT authentication is
> not implemented yet (all endpoints are currently open).

## Tech stack

- Java 21
- Spring Boot 3 (Web, Data JPA, Security, Validation)
- PostgreSQL + Flyway (schema migrations)
- springdoc-openapi (Swagger UI)
- JJWT (JWT token handling)
- Testcontainers (integration tests against a real PostgreSQL instance)
- Lombok
- Maven

## Package structure

```
com.eugeniokg.corebankingledger
├── config       # application configuration (beans, OpenAPI, etc.)
├── security     # authentication, authorization, JWT
├── account      # account domain
├── transaction  # transaction/ledger movement domain
├── audit        # audit trail and compliance
└── common       # shared building blocks (base entities, exceptions, utilities)
```

## Development setup

### Prerequisites

- JDK 21
- Docker (for local PostgreSQL and Testcontainers tests)
- Maven (or the `mvnw` wrapper included in the project)

### Starting the local database

```bash
docker compose up -d
```

Starts a PostgreSQL container on `localhost:5432` with:
- database: `core_banking_ledger`
- user: `ledger`
- password: `ledger`

### Running the application

```bash
mvn spring-boot:run
```

The `dev` profile is active by default, connecting to the local PostgreSQL instance started
via Docker Compose. The API will be available at `http://localhost:8080` and the OpenAPI
documentation at `http://localhost:8080/swagger-ui.html`.

### Tests

Integration tests use the `test` profile and Testcontainers to spin up an ephemeral
PostgreSQL instance (requires Docker to be running):

```bash
mvn test
```

## Application profiles

| Profile | Description                                          |
|---------|-------------------------------------------------------|
| `dev`   | Local PostgreSQL (Docker Compose), for development     |
| `test`  | PostgreSQL started dynamically via Testcontainers       |

## Transfers

`POST /transactions` moves money between two accounts using double-entry bookkeeping: every
transfer creates a `Transaction` plus two `LedgerEntry` records (a `DEBIT` on the source account
and a `CREDIT` on the destination account). Account balances are a cache that is only ever
updated as a side effect of recording these entries.

- Send an `Idempotency-Key` header with every request. Repeating the same key returns the
  original result instead of executing the transfer again, even under concurrent duplicate
  submissions.
- Concurrent transfers touching the same account are protected by optimistic locking
  (`@Version` on `Account`) and are retried automatically with a configurable backoff
  (`app.transaction.retry.*` in `application.yml`) if they conflict.
- A transfer that would leave the source account with a negative balance fails with a
  `409 Conflict` and a clear error message; nothing is persisted for that attempt.

## Roadmap

- [x] Domain model for `account`/`customer` (entities, repositories, not-found exceptions)
- [x] Flyway migration for the `customers`/`accounts` schema
- [x] Domain model for `transaction` (double-entry ledger, idempotency, optimistic locking)
- [x] `POST /transactions` endpoint
- [ ] REST endpoints for account/customer management
- [ ] JWT authentication and user management
- [ ] Audit trail for operations
- [ ] OpenAPI documentation for endpoints
