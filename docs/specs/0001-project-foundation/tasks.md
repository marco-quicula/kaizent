# Tasks 0001: Project foundation

- [x] **T1:** Create the parent POM, the Maven Wrapper and empty `kaizent-server` and
  `kaizent-console` modules, and pin the versions (ADR-0018). *Done when:* `./mvnw
  verify` succeeds on empty modules. *Covers:* AC-12.
- [x] **T2:** Add `.gitignore`, `.editorconfig`, `.env.example` and `compose.yaml`
  (PostgreSQL 18). *Done when:* `docker compose up -d` gives a healthy database.
  *Covers:* AC-2, AC-10 (part).
- [x] **T3:** Spring Boot application with the datasource, Flyway baseline, Actuator and
  `127.0.0.1` binding, plus an integration test with Testcontainers. *Done when:* the test
  asserts health `UP` and the applied migration. *Covers:* AC-3, AC-5.
- [x] **T4:** Spring Modulith dependency and `ModularityTest`. *Done when:* the test
  passes and a violation fixture is detected. *Covers:* AC-6.
- [x] **T5:** Spotless (google-java-format) wired into `verify`. *Done when:* unformatted
  code fails the build. *Covers:* AC-7 (Java).
- [x] **T6:** `api/openapi.yaml` with `GET /system`, generated server interfaces and
  `SystemController`. *Done when:* the API test passes. *Covers:* AC-8 (server).
- [x] **T7:** Console scaffold (Vite, React, TypeScript, Mantine, React Router, TanStack
  Query, ESLint, Prettier, Vitest), generated API types, a shell layout with placeholder
  routes, and a System page that calls `/api/v1/system`. *Done when:* `npm test` and
  `npm run build` pass. *Covers:* AC-4, AC-7 (console), AC-8 (console).
- [x] **T8:** Package the console as a resources jar and serve it from the server,
  including the SPA fallback to `index.html` for client-side routes. *Done when:* `/` and
  `/runs` load the console from the running app. *Covers:* AC-1, AC-4.
- [x] **T9:** GitHub Actions `ci.yml` and the Dependabot configuration. *Done when:* the
  workflow is green on the PR. *Covers:* AC-9, AC-10 (part).
- [x] **T10:** `CONTRIBUTING.md` (DCO, workflow, specs), `SECURITY.md`,
  `CODE_OF_CONDUCT.md`, and issue and PR templates. *Covers:* AC-10.
- [x] **T11:** README "Getting started" and a "Repository settings" checklist for the
  maintainer. *Done when:* the maintainer completes the walkthrough on a fresh clone.
  *Covers:* AC-11.
