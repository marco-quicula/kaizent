# ADR-0030: CLI tools run in containers

- **Status:** Accepted
- **Date:** 2026-11-03
- **Supersedes:** the `LocalProcessExecutor` part of [ADR-0016](0016-llm-access-api-and-cli-adapters.md)

## Context

ADR-0016 runs the CLI tools (Codex CLI, Claude Code, Antigravity, Gemini CLI) through a
`CliExecutor` seam, with a `LocalProcessExecutor` in wave 1 that starts the tool as a
local process. While planning spec 0004, three problems with that showed up:

- **Different behavior per machine.** The maintainer works on Windows; CI runs on Linux.
  Shells, paths, installation and tool versions differ, so a CLI call and its tests would
  not behave the same everywhere.
- **The sandbox is only the tool's.** A local process sees the whole machine. Read-only
  access to the workspace depends on each tool's own sandbox flags.
- **Credentials live in the user's home folder**, next to everything else the user owns.

Docker is already required to run Kaizent locally (PostgreSQL) and its tests
(Testcontainers). Wave 5 plans an agent runner that runs the tools in Kubernetes pods.

## Decision

- **Every CLI call runs in a new container.** `CliExecutor` has one wave 1
  implementation, `DockerCliExecutor`, which runs `docker run --rm` with the tool's image
  and removes the container when the call ends. It replaces `LocalProcessExecutor`.
  Kaizent calls the `docker` command line; it needs no Docker library.
- **The container contract** is the same for every tool:
  - **only the workspace paths that the call grants** are mounted, read-only, each at
    its own relative path under a fixed `/workspace` folder, which is the working
    directory. An agentic task gets only its role's knowledge roots and allowed skill
    folders (spec 0008 AC-4); a single structured call gets none. The rest of the
    workspace (`inbox/`, `outbox/`, `roles/`, other knowledge and skills, and the
    evaluation cases in `evals/`) is never visible, so a tool cannot step outside its
    role or read the expected answers of an evaluation;
  - one writable scratch folder per call holds the prompt, the output schema and the
    tool's output files, and is deleted afterwards;
  - the tool's credential volume (below) is mounted where the tool keeps its login;
  - no other host path is mounted, and the Docker socket is never mounted;
  - the environment holds only the variables that the preset declares;
  - the container runs as a non-root user, with configurable CPU and memory limits;
  - the mandatory timeout stops the container (`docker kill`) and raises `TIMEOUT`;
    output stays capped in size.
  The tool's own read-only and sandbox flags stay on: the container adds a second
  boundary, it does not replace the first.
- **Images.** Each tool has a Dockerfile in the repository (`docker/cli/<tool>/`), with
  the tool's version pinned. The images are built locally (`docker compose`, profile
  `cli`); Kaizent publishes no images. A preset names its image.
- **Credentials.** Each tool has its own named Docker volume (for example,
  `kaizent-cli-codex`). The user fills it once by running the tool's own login inside a
  container, with a documented command such as `docker compose run --rm codex-cli login`.
  Kaizent mounts the volume but never reads, copies or logs its contents. A tool that
  accepts an API key reads it from `.env`, and only that tool's container receives it.
  The volume is for the login only: a preset runs its tool in the tool's ephemeral or
  no-history mode where it has one, and points the tool's other state (sessions,
  history, caches) to the per-call scratch folder or a temporary file system, so that
  nothing from one call is left for the next. Validating a preset includes checking that
  the volume does not accumulate session files between calls.
- **Docker is required only for the CLI adapters.** The API and fake adapters do not use
  it.
- **Tests** use a small fake CLI image built from the test resources, so the
  `DockerCliExecutor` and the CLI adapters behave and are tested the same on every
  operating system.
- **Path to Kubernetes (wave 5).** The `RemoteRunnerExecutor` of ADR-0016 runs the same
  images under the same container contract, as one Kubernetes Job or Pod per call. The
  presets, the output parsing and the credential model carry over; only the place that
  starts the container changes.
- Unchanged from ADR-0016: the presets, the two ports, the cost type `SUBSCRIPTION`, and
  the terms of use. The CLI adapters remain a local, personal development mode, and each
  user checks their provider's terms.

## Alternatives considered

- **Local processes (ADR-0016).** No container overhead, but behavior differs per
  operating system, the tools must be installed on the host, and the only sandbox is the
  tool's own.
- **One long-running container per tool, called with `docker exec`.** Saves the start-up
  time, but state and files can leak from one call to the next, and a hung call is harder
  to stop cleanly.
- **Images published by the project to a registry.** Faster to start, but adds a registry
  and a release process to maintain, and users would run images they did not build.
- **Kubernetes now.** It belongs to wave 5; a local cluster is too heavy for the MVP.

## Consequences

- Each CLI call costs about a second more for container start-up. That is acceptable for
  one user processing one ticket at a time.
- Docker must be running to use a CLI adapter; Kaizent reports a clear error when it is
  not.
- The Dockerfiles must be kept up to date as the tools change. Their pinned versions make
  the presets reproducible, and a preset declares the tool version it was validated with.
- Logging in to a tool is a one-time setup step per tool, documented in the README.
