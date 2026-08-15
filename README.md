# Base project spring boot

[![Build Status](https://travis-ci.org/codecentric/springboot-sample-app.svg?branch=master)](https://travis-ci.org/codecentric/springboot-sample-app)
[![Coverage Status](https://coveralls.io/repos/github/codecentric/springboot-sample-app/badge.svg?branch=master)](https://coveralls.io/github/codecentric/springboot-sample-app?branch=master)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

Project with basic functionalities

## Requirements

For building and running the application you need:

- [JDK 25](https://www.oracle.com/java/technologies/downloads/#java25)
- [Maven 3](https://maven.apache.org)

## Running the application locally

`application.properties` requires every sensitive value (JWT secret, datasource credentials, CORS
origins) to come from an environment variable, with no defaults — this is intentional, so the app
refuses to start if one is missing. For local development, activate the `dev` profile instead,
which supplies safe local-only defaults via `application-dev.properties`:

```shell
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

Running without the `dev` profile (e.g. in staging/production) requires setting `DB_URL`,
`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ACCESS_TOKEN_EXPIRATION`,
`JWT_REFRESH_TOKEN_EXPIRATION`, `JWT_ISSUER` and `CORS_ALLOWED_ORIGINS`. Generate a JWT secret with:

```shell
openssl rand -base64 32
```

## Project structure

Classic layered architecture, package-by-layer:

- `controller/` — REST endpoints
- `usecase/` — single-purpose business operations (annotated `@UseCase` instead of `@Service`),
  each with one public `execute(...)` method
- `repository/` — Spring Data JPA repository interfaces
- `domain/` — JPA entities (`User`, `RefreshToken`, `Role`)
- `dto/` — request/response payloads (+ `dto/validation` for the custom `@ValidPassword` check)
- `security/` — JWT issuance/validation and Spring Security wiring (`security/jwt` subpackage)
- `config/` — `SecurityConfig`, `CorsProperties`
- `exception/` — `GlobalExceptionHandler` and domain exceptions

Entity primary keys are `UUID`s (`GenerationType.UUID`), not auto-increment integers.

Persistence uses Spring Data JPA. Liquibase owns the schema — Hibernate only validates entities
against it at startup (`spring.jpa.hibernate.ddl-auto=validate`), never generates or alters DDL.

Code is auto-formatted by `formatter-maven-plugin` on every `mvn compile`.

## Testing

```shell
SPRING_PROFILES_ACTIVE=dev mvn test
```

Requires Docker (tests use Testcontainers for a real Postgres instance). The suite has three
layers:

- **Unit tests** (`usecase/`, `security/jwt/`, `dto/validation/`) — business logic in isolation,
  with Mockito standing in for repositories/collaborators.
- **`ApplicationTests`** — verifies the full Spring context boots against a real, Liquibase-migrated
  database.
- **`integration/AuthFlowIntegrationTest`** — drives the real HTTP stack (MockMvc) through
  register/login/refresh/logout, including token rotation, disabled-account handling, and
  role-based access control.

## Copyright

Released under the Apache License 2.0. See the [LICENSE](https://github.com/codecentric/springboot-sample-app/blob/master/LICENSE) file.