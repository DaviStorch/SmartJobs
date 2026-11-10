# Project structure

SmartJobs is a single Spring Boot application with two source areas, not two separately deployed applications. The browser UI remains server-rendered with Thymeleaf. Maven assembles backend resources and frontend source files into the application's classpath, so existing routes, template names, static URLs, PWA paths, and deployment remain unchanged.

```text
SmartJobs/
├── backend/
│   └── src/
│       ├── main/java/com/storch/smartjobs/
│       │   ├── config/       # Spring MVC, locale and security configuration
│       │   ├── controller/   # HTTP routes and view coordination
│       │   ├── dto/          # Request/form objects
│       │   ├── entity/       # JPA entities and domain enums
│       │   ├── integration/  # External-provider contracts and adapters
│       │   ├── repository/   # Spring Data persistence
│       │   └── service/      # Business rules and workflows
│       ├── main/resources/
│       │   ├── application.properties
│       │   └── db/migration/ # Append-only Flyway migrations
│       └── test/java/        # JUnit and Mockito tests
├── frontend/
│   └── src/
│       ├── main/resources/
│       │   ├── templates/    # Thymeleaf pages and reusable fragments
│       │   ├── messages*.properties
│       ├── styles/           # Modular CSS source
│       ├── scripts/          # Browser JavaScript and service worker source
│       └── assets/           # PWA manifest, offline fallback, images
├── docs/                     # Architecture and integration documentation
├── pom.xml                   # Root Maven project; source/resource mapping
├── mvnw, mvnw.cmd            # Root Maven wrapper
└── compose.yaml
```

## Adding features

- Add controllers, DTOs, services, repositories, entities, or provider adapters under `backend/src/main/java/com/storch/smartjobs/` in the matching existing package. Keep ownership checks and business rules in services.
- Add page templates under `frontend/src/main/resources/templates/`; reuse `fragments/layout.html` and shared fragments for the shell, sidebar, header, and notifications. The Dashboard is `templates/dashboard.html`.
- Add global design tokens and reset rules in `frontend/src/styles/global.css`; use `layout.css`, `components.css`, `pages.css`, and `responsive.css` for their corresponding concerns. The shared Thymeleaf layout loads these in cascade order.
- Add browser behavior to `frontend/src/scripts/app.js`; service-worker source is `frontend/src/scripts/sw.js`.
- Add locale keys to both message bundles under `frontend/src/main/resources/`.
- Add database changes as new files in `backend/src/main/resources/db/migration/`; do not edit an applied migration.
- Put integration setup and operational notes in `docs/integrations/`.

The root `pom.xml` maps Java and tests from `backend/`, keeps backend configuration/migrations as backend resources, and maps frontend templates, translations, styles, scripts, and assets to their established classpath locations. No frontend framework, Node build, or extra dependency is required.
