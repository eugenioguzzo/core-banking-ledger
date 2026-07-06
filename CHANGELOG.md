# Changelog

Tutte le modifiche rilevanti a questo progetto sono documentate in questo file.

Il formato è basato su [Keep a Changelog](https://keepachangelog.com/it/1.1.0/),
e questo progetto aderisce a [Semantic Versioning](https://semver.org/lang/it/).

## [0.1.0] - 2026-07-06

### Added
- Scaffolding iniziale del progetto Spring Boot 3 (Java 21), Maven, packaging jar.
- Struttura a package per dominio: `config`, `security`, `account`, `transaction`, `audit`, `common`.
- Dipendenze: Spring Web, Spring Data JPA, Spring Security, Validation, driver PostgreSQL,
  Flyway, springdoc-openapi (Swagger UI), JJWT, Testcontainers, Lombok.
- `application.yml` con profili `dev` (PostgreSQL locale) e `test` (Testcontainers).
- `docker-compose.yml` per l'ambiente PostgreSQL di sviluppo locale.
- Maven Wrapper (`mvnw` / `mvnw.cmd`).
- `.gitignore` per progetti Java/Maven/IDE.
- README iniziale con descrizione del progetto e istruzioni di setup.
