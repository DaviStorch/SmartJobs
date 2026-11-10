# SmartJobs engineering guide

## Architecture and source ownership

SmartJobs is a single Spring Boot 4.1.2 application targeting Java 21. The repository separates source ownership into two directories without splitting deployment: Spring MVC and Thymeleaf still serve the pages, and Maven assembles both areas into the application classpath.

- `backend/src/main/java/com/storch/smartjobs/config`: Spring Security, MVC, locale, and application configuration.
- `backend/src/main/java/com/storch/smartjobs/controller`: HTTP routes and view coordination.
- `backend/src/main/java/com/storch/smartjobs/dto`: validated request/form objects.
- `backend/src/main/java/com/storch/smartjobs/entity`: JPA entities and domain enums.
- `backend/src/main/java/com/storch/smartjobs/repository`: Spring Data persistence and account-scoped queries.
- `backend/src/main/java/com/storch/smartjobs/service`: business workflows and domain logic.
- `backend/src/main/java/com/storch/smartjobs/integration`: external provider contracts and adapters, including Gmail and AI.
- `backend/src/main/resources`: application configuration and Flyway migrations.
- `backend/src/test/java`: automated tests.
- `frontend/src/main/resources/templates`: Thymeleaf pages and reusable fragments.
- `frontend/src/main/resources/messages*.properties`: Portuguese and English message bundles.
- `frontend/src/styles`: modular CSS source files.
- `frontend/src/scripts`: browser behavior and service-worker source.
- `frontend/src/assets`: images and PWA assets.

The root `pom.xml` maps Java and tests from `backend/` and places frontend resources at the same classpath URLs used by Spring (`templates/`, `static/`, and message bundles). Keep the root Maven Wrapper and project file so existing root-level Maven commands continue working.

For detailed directory guidance, see `docs/architecture/structure.md`.

## Development conventions

- Scope every user-owned data query to the authenticated user; do not load private records by ID alone in web workflows.
- Put business rules and ownership checks in services. Validate forms at the boundary and validate file size/type/content before parsing.
- Encode passwords with the configured `PasswordEncoder`; keep CSRF enabled for browser forms.
- Keep compatibility scoring deterministic and explainable. Never infer or fabricate a candidate skill or qualification.
- Add database changes as new Flyway migrations. Keep `spring.jpa.hibernate.ddl-auto=validate`; do not use `create` or `create-drop`.
- Keep runtime credentials in environment variables. Compose defaults are local-development placeholders only.
- Preserve server-rendered Thymeleaf and small browser-native JavaScript; no SPA framework or frontend package build is used.
- Add focused JUnit/Mockito tests for service rules and PostgreSQL-backed tests only when a disposable database is available.

## Features and external requirements

- Signup/login/logout, BCrypt, CSRF, account-private job/resume records, and saved theme/language preferences.
- Job management, deterministic compatibility analysis, dashboard/statistics, editable resume modules, PDF import review, optional Tesseract OCR, Gmail OAuth/import, optional OpenAI-compatible AI, and user-controlled application preparation.
- OCR needs an external Tesseract installation. Gmail needs Google OAuth client configuration and a stable encryption key. AI is opt-in and requires a configured local or remote compatible endpoint.
- Application preparation does not submit applications. CAPTCHA bypass, scraping, and unattended application submission are prohibited.
- The service worker caches public assets and an offline fallback only; authenticated pages, user data, and credentials must never be cached.

## Commands

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify -q
.\mvnw.cmd spring-boot:run
```
