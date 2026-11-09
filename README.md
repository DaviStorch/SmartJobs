# SmartJobs

SmartJobs is a private career workspace for recording job opportunities, tracking applications, organizing resume versions, and comparing job requirements with a resume using an explainable, deterministic score.

## Features

- Personal accounts with BCrypt password hashing, CSRF protection, and account-scoped records.
- Job creation, editing, deletion, search, status/source filters, pagination, sorting, duplicate checks, notes, and source links.
- Dashboard and statistics calculated from the signed-in user's PostgreSQL records.
- Editable resume sections, multiple versions, active resume selection, and PDF text extraction (not OCR; formatting and extraction can be incomplete).
- Requirement analysis with matched, partially matched, and unrecognized requirements; no external AI/API key is needed.
- Saved light/dark theme and Portuguese/English locale preferences.
- Installable PWA shell with an offline fallback page. Protected application data still requires the server and database.

## Requirements

- Java 21
- Docker with Docker Compose (recommended for local PostgreSQL), or a reachable PostgreSQL 15+ server
- Maven Wrapper included in this repository

The project uses Spring Boot 4.1.2 and Maven. No frontend build or paid service is required.

## Run locally

The Docker Compose integration starts the repository's PostgreSQL service when the application starts. The defaults are for local development only and the database port is bound locally:

```powershell
.\mvnw.cmd spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) and create an account. Flyway applies migrations automatically; Hibernate validates the schema and does not create or drop tables.

To run PostgreSQL separately or use a custom database, configure:

| Variable | Default | Description |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/smartjobs` | JDBC URL |
| `DATABASE_USERNAME` | `smartjobs` | PostgreSQL account |
| `DATABASE_PASSWORD` | `smartjobs` | Local development password; replace outside local development |
| `POSTGRES_DB` | `smartjobs` | Database name used by Compose |
| `POSTGRES_PORT` | `5432` | Host port for Compose |
| `PORT` | `8080` | HTTP port |
| `THYMELEAF_CACHE` | `false` | Enable template caching in production |
| `SESSION_COOKIE_SECURE` | `false` | Set to `true` behind HTTPS in production |

Compose uses the database variables above. If you change the database name or host port, update `DATABASE_URL` accordingly. Do not commit credentials or production secrets.

For a database managed outside Compose, set `spring.docker.compose.enabled=false` (for example with `SPRING_DOCKER_COMPOSE_ENABLED=false`) and provide the database variables.

## Tests and build

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

Unit tests cover account registration, ownership checks, duplicate detection, pasted job text parsing, and compatibility scoring. The application requires PostgreSQL at runtime; tests that need a live persistence context should use a disposable PostgreSQL instance and the Flyway migrations.

## Notes and limitations

- Gmail ingestion is not enabled. OAuth consent, scopes, token storage, and a user-authorized integration are prerequisites; the app does not access mailboxes or scrape job sites.
- PDF import extracts selectable text only. Scanned documents need OCR, which is intentionally not bundled.
- The analysis is a transparent keyword/phrase comparison, not a hiring prediction. A zero score for a job without structured requirements means there was insufficient data.
- The service worker caches static assets and an offline message only. It does not cache private pages or promise offline database functionality.
- Production deployment must provide HTTPS, strong per-environment credentials, database backups, and an appropriate reverse-proxy/session-cookie configuration.
