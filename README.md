# SmartJobs

SmartJobs is a private career workspace for recording job opportunities, tracking applications, organizing resume versions, and comparing job requirements with a resume using an explainable, deterministic score.

## Features

- Personal accounts with BCrypt password hashing, CSRF protection, and account-scoped records.
- Job creation, editing, deletion, search, status/source filters, pagination, sorting, duplicate checks, notes, and source links.
- Dashboard and statistics calculated from the signed-in user's PostgreSQL records.
- Editable resume sections, multiple versions, active resume selection, editable PDF-import preview, and optional scanned-PDF OCR through a local Tesseract installation.
- Requirement analysis with matched, partially matched, and unrecognized requirements; no external AI/API key is needed.
- Optional AI explanations through an OpenAI-compatible chat-completions endpoint. AI receives only job data and existing deterministic analysis; it never calculates or changes the compatibility score.
- Gmail OAuth 2.0 read-only integration with encrypted tokens, incremental alert search, duplicate prevention, and review-oriented import.
- External application preparation that organizes the active resume and opens the original job link; SmartJobs does not submit applications.
- Saved light/dark theme and Portuguese/English locale preferences.
- Installable PWA shell with an offline fallback page. Protected application data still requires the server and database.

## Requirements

- Java 21
- Docker with Docker Compose (recommended for local PostgreSQL), or a reachable PostgreSQL 15+ server
- Maven Wrapper included in this repository

The project uses Spring Boot 4.1.2 and Maven. No frontend build or paid service is required.

## Project layout

The code is split into `backend/` and `frontend/` source areas while remaining one Spring Boot deployment. Thymeleaf pages are kept server-rendered; Maven copies frontend templates, translations, styles, scripts, and PWA assets to the classpath locations served by Spring. No React application or separate frontend server is introduced.

- Backend Java and tests: `backend/src/main/java/` and `backend/src/test/java/`.
- Backend configuration and database migrations: `backend/src/main/resources/`.
- Thymeleaf pages and shared fragments: `frontend/src/main/resources/templates/`.
- CSS modules: `frontend/src/styles/`.
- Browser scripts and service worker: `frontend/src/scripts/`.
- Public images and PWA assets: `frontend/src/assets/`.
- Structure and extension guide: `docs/architecture/structure.md`.

The root `pom.xml` and Maven wrapper preserve the existing root-level build and run commands.

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
| `SMARTJOBS_OCR_TESSERACT` | empty | Optional Tesseract executable path; empty means scanned PDFs show a configuration warning |
| `SMARTJOBS_OCR_LANGUAGE` | `por+eng` | Installed Tesseract language packs to use |
| `SMARTJOBS_OCR_MAX_PAGES` | `20` | Page limit for PDF import (capped at 30) |
| `SMARTJOBS_AI_ENABLED` | `false` | Explicitly enables the optional AI request |
| `SMARTJOBS_AI_ENDPOINT` | local Ollama-compatible URL | OpenAI-compatible chat-completions endpoint; remote endpoints must use HTTPS |
| `SMARTJOBS_AI_MODEL` | `qwen2.5:3b` | Model identifier understood by the configured endpoint |
| `SMARTJOBS_AI_API_KEY` | empty | Optional provider key; do not commit it |
| `SMARTJOBS_AI_TIMEOUT_SECONDS` | `15` | AI connection/read timeout, capped at 60 seconds |
| `GOOGLE_OAUTH_CLIENT_ID` | empty | Google OAuth web-client ID |
| `GOOGLE_OAUTH_CLIENT_SECRET` | empty | Google OAuth web-client secret |
| `GOOGLE_OAUTH_REDIRECT_URI` | empty | Exact OAuth callback URL, `/integrations/gmail/callback` |
| `SMARTJOBS_GMAIL_ENCRYPTION_KEY` | empty | Secret of at least 32 bytes used to derive the AES-GCM token-encryption key |

Compose uses the database variables above. If you change the database name or host port, update `DATABASE_URL` accordingly. Do not commit credentials or production secrets.

For a database managed outside Compose, set `spring.docker.compose.enabled=false` (for example with `SPRING_DOCKER_COMPOSE_ENABLED=false`) and provide the database variables.

## Tests and build

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

Unit tests cover account registration, ownership checks, duplicate detection, pasted job text parsing, and compatibility scoring. The application requires PostgreSQL at runtime; tests that need a live persistence context should use a disposable PostgreSQL instance and the Flyway migrations.

## Notes and limitations

- The analysis is a transparent keyword/phrase comparison, not a hiring prediction. A zero score for a job without structured requirements means there was insufficient data.
- Production deployment must provide HTTPS, strong per-environment credentials, database backups, and an appropriate reverse-proxy/session-cookie configuration.

## Optional integrations

### OCR

Selectable PDF text is extracted with PDFBox. If fewer than 40 characters are found, the importer attempts OCR only when `SMARTJOBS_OCR_TESSERACT` points to an installed Tesseract executable. Install Tesseract and the Portuguese/English language data separately; no native binary or OCR dependency is bundled. Pages are rendered sequentially, bounded by page and image-size limits, and temporary files are deleted. OCR output is a draft and must be reviewed before saving.

### AI explanations

AI is off by default. A local Ollama server exposing an OpenAI-compatible chat endpoint can be configured with `SMARTJOBS_AI_ENABLED=true`; the model must already be installed separately, and local inference resource use depends on that model. A remote compatible endpoint may incur charges or have provider-specific data-retention terms; verify those terms yourself before enabling it. The request sends job content and SmartJobs' existing score/matched/missing findings, not the user's name, contact details, or raw resume. Requests are length-limited, have a timeout and per-user cooldown, and fall back to the deterministic explanation. No AI provider is bundled or represented as a free hosted service.

### Gmail OAuth

1. In Google Cloud Console, create a project, enable the Gmail API, and configure the OAuth consent screen for the intended users.
2. Create an OAuth client of type **Web application** and register the exact redirect URI `https://<your-host>/integrations/gmail/callback` (use `http://localhost:8080/integrations/gmail/callback` only for local development).
3. Configure `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`, `GOOGLE_OAUTH_REDIRECT_URI`, and a stable random `SMARTJOBS_GMAIL_ENCRYPTION_KEY` of at least 32 bytes. Keep the encryption key backed up and unchanged; tokens cannot be recovered if it is lost.
4. Apply Flyway migrations at startup, sign in, open **Integrations**, and authorize Gmail read-only access.

The connector requests only `gmail.readonly`, searches at most 50 recent messages per synchronization using job-related subject terms, requires a recognizable title and company, and preserves a detected application link. Message IDs and equivalent opportunities are deduplicated. Revoked consent requires reconnecting. Google OAuth consent/testing and verification requirements can depend on the Google project configuration; external access has not been exercised without project credentials.

### Application preparation and offline use

Application preparation shows the active resume sections and the original external link. It never autofills third-party sites, bypasses protections, or submits applications; record **Applied** only after you confirm the external submission. No official job-board submission API is configured.

The service worker caches static assets and an offline fallback only. Authenticated opportunity/resume data and mutations are not available offline; no sensitive data or authentication tokens are cached in the browser.
