# ADR-0018: Dependency version baseline and pinning policy

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

Spring Boot and Spring AI release often. At the time of writing, Maven Central lists
Spring Boot **4.1.2** and Spring AI **2.0.2** as the latest GA releases, with Spring Boot
4.2 and Spring AI 2.1 at release-candidate stage.

## Decision

- When implementation starts (spec 0001), we **pin the latest GA releases** of Spring
  Boot, Spring AI and Spring Modulith that are **compatible with each other**, as
  documented by the Spring projects. The baseline is Spring Boot 4.1.x and Spring AI
  2.0.x, or newer GA releases if they are available by then.
- Versions are managed in the parent POM through the Spring Boot parent and the Spring AI
  and Spring Modulith BOMs. No versions are scattered across modules.
- We never depend on milestone, RC or snapshot releases on `main`.
- Dependabot proposes updates (ADR-0026). Minor and patch updates are merged when CI is
  green. Major updates get their own PR and, when they change behavior, a note in the
  relevant spec or ADR.
- Other baselines: **Java 25 (LTS)**, **Node.js 24 (LTS)** for the console build,
  **PostgreSQL 18** in `docker compose` and Testcontainers.

## Alternatives considered

- **Pin the exact versions now.** They would be outdated by the time code starts.
- **Use release candidates to get newer features.** Unstable for a project meant for
  others to run.

## Consequences

- Spec 0001 records the exact pinned versions in the parent POM.
- Contributors need JDK 25. Maven downloads Node automatically for the console build
  (ADR-0025).
