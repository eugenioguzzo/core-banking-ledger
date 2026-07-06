# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

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
