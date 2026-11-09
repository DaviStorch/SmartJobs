# SmartJobs engineering guide

## Architecture

The application is a server-rendered Spring Boot 4.1.2 service targeting Java 21. Thymeleaf handles the UI; PostgreSQL stores data; Flyway owns schema evolution. There is no separate JavaScript application build.

- `config`: Spring Security and MVC/locale setup.
- `controller`: authenticated web routes and request-to-view coordination.
- `dto`: validated form objects; persistence entities are not bound directly from forms.
- `entity`: user, opportunity, and resume records plus domain enums.
- `repository`: Spring Data persistence and account-scoped queries.
- `service`: account registration, job/resume workflows, compatibility analysis, and dashboard/statistics calculations.
- `resources/db/migration`: append-only Flyway migrations.
- `resources/templates`: Thymeleaf pages and shared fragments.
- `resources/static`: responsive styles, browser enhancements, manifest, and service worker.
- `integration`: reserved for explicit, authorized third-party connectors; Gmail OAuth has not been implemented.

## Development conventions

- Keep all user-owned repository access scoped by `user.id`; never load a private record by its ID alone in a web workflow.
- Put business rules and ownership checks in services. Validate forms at the boundary and validate file size/type/content before parsing.
- Encode passwords with the configured `PasswordEncoder`; keep CSRF enabled for browser forms.
- Keep compatibility scoring deterministic and explainable. Never infer or fabricate a candidate skill or qualification.
- Add database changes as new Flyway migrations. Keep `spring.jpa.hibernate.ddl-auto=validate`; do not use `create` or `create-drop`.
- Keep runtime credentials in environment variables. Compose defaults are local-development placeholders only.
- Prefer server-rendered HTML, CSS, and small browser-native JavaScript; no SPA framework is used.
- Add focused JUnit/Mockito tests for service rules and PostgreSQL-backed tests only when a disposable database is available.

## Existing dependencies

Spring Boot starters provide Flyway, Spring Data JPA, Security, Thymeleaf, Validation, and Spring MVC. PostgreSQL is the runtime driver. Apache PDFBox is used for selectable-text extraction from PDF files. Existing Springdoc, DevTools, Docker Compose, Lombok, and test starters remain in the Maven project.

## Implemented

- Account signup/login/logout, BCrypt, CSRF, account-private job/resume queries, and saved theme/language preferences.
- Opportunity management, filters, pagination, duplicate detection, notes, status tracking, and dashboard/statistics aggregates.
- Multi-version resumes, editable sections, PDF text extraction, and requirement compatibility analysis.
- Responsive light/dark UI, locale resources, PWA manifest/service worker, and an offline fallback.

## Pending / constraints

- Gmail integration needs an OAuth registration/configuration, consent flow, secure token persistence, and provider implementation; none is represented as working today.
- PDF OCR, external AI, automated applications, and job-board scraping are intentionally out of scope.
- English locale resources translate shared navigation, dashboard summaries, opportunity filters, and primary page headings; detailed form copy, status labels, and some notices remain in Portuguese.
- Browser-based end-to-end testing and PostgreSQL-backed integration tests require a separately available local database.

## Commands

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd spring-boot:run
```
