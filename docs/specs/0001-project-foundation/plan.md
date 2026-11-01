# Plan 0001: Project foundation

The target layout is described in ADR-0019 and `docs/architecture.md`.

## Modules and components

| Location | Component | Responsibility |
|---|---|---|
| `pom.xml` | `kaizent-parent` | Spring Boot parent, BOM imports (Spring AI, Spring Modulith), plugin management (Spotless, frontend plugin, openapi-generator), Java 25 |
| `kaizent-console/` | Vite + React + TypeScript app | Console shell with Mantine `AppShell` and React Router placeholder routes; packaged into `META-INF/resources/` |
| `kaizent-console/pom.xml` | frontend Maven plugin | Installs Node 24 and npm, runs `npm ci`, `npm run generate:api`, `npm run lint`, `npm test`, `npm run build` |
| `kaizent-server/` | `KaizentApplication` | Spring Boot app; depends on `kaizent-console` |
| `kaizent-server` | `api` module | `SystemController` implementing the generated `SystemApi` |
| `kaizent-server` | `ModularityTest` | `ApplicationModules.of(KaizentApplication.class).verify()` |
| repository root | `compose.yaml` | `postgres:18` service with a volume and a healthcheck |

## Data

- `V1__baseline.sql`: an empty baseline (for example, a comment, or the Spring Modulith
  `event_publication` table).

## API

- `api/openapi.yaml` (OpenAPI 3.1): `GET /api/v1/system` → `{ version, workspace: null,
  profiles: [] }`. Fields are filled in by later specs.

## Configuration

```yaml
server.address: 127.0.0.1
spring.datasource.url: jdbc:postgresql://localhost:5432/kaizent
spring.datasource.username: ${KAIZENT_DB_USER:kaizent}
spring.datasource.password: ${KAIZENT_DB_PASSWORD}   # no default; set in .env
management.endpoints.web.exposure.include: health,info
```

- Spring Boot's dotenv-style import (`spring.config.import: optional:file:.env[.properties]`)
  loads the local `.env`.

## Testing

| AC | Test |
|---|---|
| AC-1, AC-9 | CI run on the first PR |
| AC-3 | `@SpringBootTest` with Testcontainers PostgreSQL (`@ServiceConnection`), asserting health and Flyway |
| AC-4 | Vitest render test of the shell; manual check in the browser |
| AC-6 | `ModularityTest` |
| AC-7 | Spotless and ESLint run in `verify` |
| AC-8 | API test calling `/api/v1/system` |
| AC-11 | Manual walkthrough by the maintainer |

## Risks and mitigations

- Version compatibility between Spring Boot, Spring AI, Spring Modulith and Java 25 →
  check the projects' compatibility notes when pinning; a smoke test starts the full
  context.
- Frontend plugin and Node download in CI → cache `node/` and `~/.npm` in the workflow.
