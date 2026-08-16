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
which supplies safe local-only defaults via `application-dev.properties`.

First, start a local Postgres and apply the schema (requires Docker):

```shell
cd development-environment
make dev     # starts Postgres and runs the Flyway migrations
```

Then, from the project root:

```shell
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Other `development-environment` targets: `make up`/`make down` (start/stop Postgres only),
`make migrate` (re-run Flyway against an already-running Postgres), `make reset` (wipe the local
database volume), `make logs` (tail Postgres logs). Run `make help` to list them.

Running without the `dev` profile (e.g. in staging/production) requires setting every variable
below — nothing has a fallback outside the `dev` profile, so the app refuses to start if one is
missing:

- Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- JWT: `JWT_SECRET`, `JWT_ACCESS_TOKEN_EXPIRATION`, `JWT_REFRESH_TOKEN_EXPIRATION`,
  `JWT_MFA_TOKEN_EXPIRATION`, `JWT_ISSUER`
- CORS: `CORS_ALLOWED_ORIGINS`
- Email verification: `EMAIL_VERIFICATION_BASE_URL` (frontend route the verification link points
  to), `EMAIL_VERIFICATION_TOKEN_EXPIRATION`
- Outbound mail (SMTP): `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`,
  `MAIL_FROM_ADDRESS`
- Google OAuth2 login: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` (create both for free at the
  [Google Cloud Console](https://console.cloud.google.com/apis/credentials); redirect URI is
  `<base-url>/login/oauth2/code/google`), `OAUTH2_REDIRECT_URI` (frontend route that receives the
  issued tokens after a successful Google login)

Generate a JWT secret with:

```shell
openssl rand -base64 32
```

In the `dev` profile, the database, JWT and mail settings have safe local-only defaults (mail is
just logged to the console instead of actually sent), but `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`
still fall back to a non-functional placeholder — the app boots fine, but "Sign in with Google"
won't work until you export real values for those two.

## API documentation

The API is documented with OpenAPI 3 (springdoc-openapi). With the app running:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Raw OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Both are public endpoints (no token required). For endpoints that do require one, click
**Authorize** in Swagger UI and paste a bearer access token obtained from `/api/auth/login`.

## Project structure

Classic layered architecture, package-by-layer:

- `controller/` — REST endpoints
- `usecase/` — single-purpose business operations (annotated `@UseCase` instead of `@Service`),
  each with one public `execute(...)` method
- `repository/` — Spring Data JPA repository interfaces
- `domain/` — JPA entities (`User`, `RefreshToken`, `EmailVerificationToken`, `Role`)
- `dto/` — request/response payloads (+ `dto/validation` for the custom `@ValidPassword` check)
- `security/` — JWT issuance/validation and Spring Security wiring (`security/jwt`), TOTP 2FA
  (`security/totp`), Google login (`security/oauth2`)
- `email/` — outbound email abstraction (`EmailSender`): logs to the console in `dev`, real SMTP
  otherwise
- `config/` — `SecurityConfig` and the app's `@ConfigurationProperties` records (CORS, email
  verification, OAuth2 redirect)
- `exception/` — `GlobalExceptionHandler` and domain exceptions

Entity primary keys are `UUID`s (`GenerationType.UUID`), not auto-increment integers.

Persistence uses Spring Data JPA. Flyway owns the schema (migrations in
`src/main/resources/db/migration`) — Hibernate only validates entities against it at startup
(`spring.jpa.hibernate.ddl-auto=validate`), never generates or alters DDL.

Code is auto-formatted by `formatter-maven-plugin` on every `mvn compile`.

## Testing

```shell
SPRING_PROFILES_ACTIVE=dev mvn test
```

Requires Docker (tests use Testcontainers for a real Postgres instance). The suite has three
layers:

- **Unit tests** (`usecase/`, `security/jwt/`, `dto/validation/`) — business logic in isolation,
  with Mockito standing in for repositories/collaborators.
- **`ApplicationTests`** — verifies the full Spring context boots against a real, Flyway-migrated
  database.
- **`integration/AuthFlowIntegrationTest`** — drives the real HTTP stack (MockMvc) through
  register/login/refresh/logout, including token rotation, disabled-account handling, and
  role-based access control.

## Copyright

Released under the Apache License 2.0. See the [LICENSE](https://github.com/codecentric/springboot-sample-app/blob/master/LICENSE) file.