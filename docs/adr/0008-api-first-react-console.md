# ADR-0008: API-first backend with a React single-page console

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

A human must review every proposal in the MVP, so a console for monitoring runs and
giving verdicts is part of the minimal cycle. It must show the queue, the full trace of a
run and the review actions. The console will grow richer over time (dashboards,
real-time progress, autonomy management).

## Decision

- The backend is **API-first**: a REST/JSON API documented with **OpenAPI** is the
  official contract for every UI and integration.
- The console is a separate **single-page application (SPA)** written in **React +
  TypeScript**, built with **Vite**, and kept in its own folder in this repository.
- For simple local runs, the SPA build is **bundled into the Spring Boot artifact** and
  served as static files, so one command starts everything. In Kubernetes it can later
  be deployed as its own container.
- Live run progress uses Server-Sent Events (SSE) when needed.

## Alternatives considered

- **Server-side rendering (Thymeleaf + htmx).** One stack, and the lowest cost at
  first. Rejected because the screens would have to be rewritten when the console grows,
  and the maintainer prefers a frontend that consumes an API.
- **Angular.** It is well structured and familiar to Java developers. React was chosen
  for its larger community and contributor pool.

## Consequences

- Contributors working on the console need Node.js. Backend-only contributors do not.
  The Maven build can run the frontend build through a plugin.
- The API must be designed and versioned deliberately from the MVP onward.
- The console has no authentication in the MVP (it runs locally only). Authentication
  arrives in wave 5.
