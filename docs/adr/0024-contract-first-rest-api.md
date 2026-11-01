# ADR-0024: Contract-first REST API with OpenAPI

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

ADR-0008 made the backend API-first, with a React console as its first client. The build
order (the console is built before the server, ADR-0019) and the goal of a stable,
reviewable contract favor writing the contract before the code.

## Decision

- The REST contract is a hand-written **OpenAPI 3.1** document at `api/openapi.yaml`.
  It is the source of truth.
- The server generates **Java interfaces and DTOs** from the contract
  (openapi-generator, interface-only mode) and implements them.
- The console generates **TypeScript types** from the same contract (`openapi-typescript`)
  and calls the API through a small typed client (`openapi-fetch`).
- The API is versioned in the path (`/api/v1`). Breaking changes require a new version
  or an explicitly approved spec.
- The contract is served at runtime, and the build validates it against the generated
  code.

## Alternatives considered

- **Code-first with springdoc.** Less upfront writing, but the console build would depend
  on running the server, and the contract would drift with the code instead of being
  reviewed.

## Consequences

- Every API change starts with a change to `api/openapi.yaml`, which is reviewed in the
  PR.
- The generator tools are Apache-2.0 (openapi-generator) and MIT (openapi-typescript,
  openapi-fetch), both compatible with our license.
