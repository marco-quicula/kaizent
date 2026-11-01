# ADR-0019: Repository and Maven module layout

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

ADR-0006 chose a modular monolith with ports and adapters, enforced by Spring Modulith.
ADR-0008 chose a React console bundled into the Spring Boot artifact. We need a concrete
layout that keeps one-command builds and leaves room to extract a connector SPI later.

## Decision

```
kaizent/
├── pom.xml                  # parent: br.com.refatorado.kaizent:kaizent-parent
├── mvnw, .mvn/              # Maven Wrapper
├── api/openapi.yaml         # REST contract (ADR-0024)
├── kaizent-console/         # React + TypeScript SPA, packaged as a jar of static resources
├── kaizent-server/          # Spring Boot application (single Maven module, Modulith modules inside)
├── examples/petclinic/      # demo workspace (ADR-0027, spec 0003)
├── compose.yaml             # local PostgreSQL
├── docs/                    # vision, roadmap, ADRs, specs, architecture
└── .github/                 # CI, templates, Dependabot
```

- **`kaizent-server`** is a single Maven module. Its Spring Modulith application modules
  are the top-level packages under `br.com.refatorado.kaizent` (see
  `docs/architecture.md`). Module boundaries are verified by a Modulith test.
- **`kaizent-console`** is built by the Maven build (frontend plugin) and packaged as a
  jar whose static files sit under `META-INF/resources/`. `kaizent-server` depends on it,
  so the application serves the console at `/`.
- **Formatting:** Spotless with google-java-format for Java, and Prettier and ESLint for
  the console. Formatting is checked in CI.
- **No per-file license headers.** The root `LICENSE` file covers the repository.
- A separate connector SPI module is extracted only when external adapters need it
  (wave 2 or later).

## Alternatives considered

- **One Maven module per hexagon layer or adapter.** Many modules, slower builds and
  boilerplate. Spring Modulith enforces the same boundaries inside one module.
- **Console as a separate deployable from day one.** More moving parts for local runs.
  It remains possible later (ADR-0008).

## Consequences

- `./mvnw verify` builds and tests everything. `./mvnw -pl kaizent-server` works for
  backend-only work when the console jar is already installed.
- Module boundary violations fail the build.
