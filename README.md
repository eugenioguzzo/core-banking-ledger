# core-banking-ledger

Scaffolding for a core banking ledger service, built with Spring Boot 3 and Java 21.
The project will expose REST APIs for managing accounts, transactions and audit trails,
with JWT-based authentication and PostgreSQL persistence.

> Current status: technical scaffolding only (build, configuration, package structure).
> No domain logic has been implemented yet.

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

## Roadmap

- [ ] Domain model for `account` and `transaction`
- [ ] Flyway migrations for the ledger schema
- [ ] JWT authentication and user management
- [ ] Audit trail for operations
- [ ] OpenAPI documentation for endpoints
