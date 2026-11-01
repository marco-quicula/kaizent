# Spec 0001: Project foundation

- **Status:** In progress
- **Wave:** 1
- **Depends on:** none
- **Related ADRs:** 0002, 0005, 0006, 0009, 0018, 0019, 0024, 0025, 0026

## Goal

Create the repository skeleton that every later spec builds on: the Maven build with both
modules, local PostgreSQL, an application that starts and serves an empty console, CI,
and the open-source hygiene files. There is no domain behavior yet.

## User stories

- As a **contributor**, I want to clone the repository and build it with one command, so
  that I can start contributing without setup guesswork.
- As the **maintainer**, I want CI to check every pull request, so that `main` always
  builds.
- As a **newcomer**, I want the README to tell me how to run the project, so that I can
  try it in minutes.

## Acceptance criteria

- **AC-1:** Given a clean clone with JDK 25 and Docker, when `./mvnw verify` runs, then
  it builds `kaizent-console` and `kaizent-server` and all tests pass. No global Node
  installation is needed.
- **AC-2:** Given `docker compose up -d`, then PostgreSQL 18 is reachable with the
  credentials documented in `.env.example`.
- **AC-3:** Given the database is up, when the application starts, then
  `/actuator/health` reports `UP` (including the database) and Flyway has applied the
  baseline migration.
- **AC-4:** When a browser opens `http://localhost:8080/`, then the console shell loads,
  with the Kaizent name and placeholder navigation (Runs, Lessons, Subjects, Intake,
  System).
- **AC-5:** The application binds to `127.0.0.1` by default.
- **AC-6:** A Spring Modulith verification test exists and passes, and a deliberate
  violation in a test fixture is detected.
- **AC-7:** The build fails on unformatted Java code (Spotless) and on console lint or
  format errors.
- **AC-8:** `api/openapi.yaml` exists with the `/api/v1` base and a `GET /system`
  operation. The server implements it from generated interfaces, and the console
  generates its types from it. `GET /api/v1/system` returns the application version.
- **AC-9:** A GitHub Actions workflow runs `./mvnw -B verify` on pull requests and on
  pushes to `main`, without any secrets.
- **AC-10:** The repository contains `CONTRIBUTING.md` (including DCO sign-off),
  `SECURITY.md`, `CODE_OF_CONDUCT.md`, issue and PR templates, a Dependabot configuration
  (Maven, npm, Actions), `.editorconfig`, `.gitignore` (including `.env`) and
  `.env.example`.
- **AC-11:** The README has a "Getting started" section whose steps produce AC-2 to AC-4
  on a fresh machine.
- **AC-12:** Spring Boot, Spring AI and Spring Modulith versions are pinned once in the
  parent POM, following ADR-0018.

## Evaluation cases

None.

## Out of scope

- Any domain module behavior (spec 0002 onwards).
- Container images and Kubernetes (wave 5).
- Enabling the repository settings (DCO app, secret scanning, branch protection). The
  maintainer does this; the README lists the steps.

## Open questions

None.
