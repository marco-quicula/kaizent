# ADR-0026: CI and repository automation

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

A public repository needs automated checks for every pull request, protection against
leaked secrets and a clear contribution policy (DCO, ADR-0002).

## Decision

- **GitHub Actions** workflow `ci.yml` runs on pull requests and on pushes to `main`:
  - JDK 25 (Temurin), with the Maven cache;
  - `./mvnw -B verify`: backend and console builds, all tests (Testcontainers runs on
    the hosted Linux runners), formatting checks and the Modulith verification;
  - no LLM credentials and no evaluations (ADR-0020).
- **DCO check:** the DCO GitHub App verifies a `Signed-off-by` line on every commit of a
  pull request.
- **Dependabot** for Maven, npm and GitHub Actions, on a weekly schedule.
- **Secret scanning with push protection**, enabled in the repository settings (free for
  public repositories).
- Branch protection on `main`: CI and the DCO check must pass, and pull requests are
  merged with **Squash and merge**.
- Repository files: `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md` (Contributor
  Covenant), and issue and pull request templates.

## Alternatives considered

- **Another CI service.** GitHub Actions is free for public repositories and sits next
  to the code.
- **A CLA bot.** Not needed with the DCO.

## Consequences

- The maintainer enables, in the repository settings: the DCO app, secret scanning with
  push protection, and branch protection. These steps cannot be automated from the
  repository.
- CodeQL or other scanners can be added later without changing this decision.
