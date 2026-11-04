# ADR-0031: CLI tool state stays in the container; the image may move the login

- **Status:** Accepted
- **Date:** 2026-11-04
- **Supersedes:** the credentials part of [ADR-0030](0030-cli-tools-run-in-containers.md)
  ("Kaizent mounts the volume but never reads, copies or logs its contents")

## Context

ADR-0030 gives each CLI tool a named Docker volume for its login. Kaizent mounts it but
"never reads, copies or logs its contents", and the tool's other state (sessions,
history, caches) goes to the per-call scratch folder or a temporary file system, so that
nothing is left from one call to the next.

The maintainer's discovery run with Codex CLI 0.166.0 (spec 0004 PR 4) showed that Codex
keeps its login, sessions, logs, SQLite state, caches and plugins in the same folder,
`CODEX_HOME`. After the login the folder held 5 files; after two calls, 598. The
`--ephemeral` flag alone did not leave it unchanged. Sessions hold the prompts, so the
tickets' text would stay in the volume between calls.

Keeping the tool's state out of the volume therefore needs `CODEX_HOME` in a temporary
folder. Codex reads its login from `CODEX_HOME` too, so the login must be put there for
each call, and a login that Codex renews must be kept. Something in the container has to
copy the login file. ADR-0030 did not foresee that.

## Decision

- **The credential boundary is the tool's container and its volume.** A CLI tool's login
  never leaves them, and **Kaizent's application never reads, copies or logs it**: it only
  mounts the volume into the tool's container.
- **The tool's image may move the login inside the container.** A small wrapper in the
  image, which is part of the project, runs the tool with its state in a private
  temporary folder under `/tmp` (a temporary file system per call). It copies only the
  login file there before the call, and writes it back to the volume only when the tool
  renewed it, through a private temporary file and an atomic rename. The wrapper never
  prints, logs or sends the login anywhere else. For Codex this is `kaizent-codex`
  (`docker/cli/codex/`).
- **The volume holds the login only.** Validating a preset includes checking that the
  volume holds nothing but the login after calls.
- **This is a narrow exception.** It covers only a CLI tool's own login file, moved by
  that tool's image wrapper inside that tool's container, to and from a private temporary
  folder that lives only for the call. It allows no other access to credentials: not by
  Kaizent's application on the host, not by other containers, and not to API keys, which
  keep reaching only the container of the tool that declares them.
- Unchanged from ADR-0030: a new container per call, the read-only root file system, the
  non-root user, no Linux capabilities, the granted read-only paths, the declared
  environment variables, the timeout, and the per-call scratch folder.

## Alternatives considered

- **Mount only the login file from the volume into the temporary folder** (a Docker
  volume subpath). Nothing would copy it: the tool would read and write it itself. It
  depends on how the tool writes the file: a tool that writes a temporary file and renames
  it over the login cannot rename onto a mounted file, so the renewal would fail, and this
  can change with any tool version. It also needs Docker 26 or later.
- **Keep the state in the volume and delete everything but the login after each call.**
  An interrupted call (timeout, crash) would leave the sessions, with the tickets' text,
  in the volume, and the wrapper would still touch the volume.
- **Accept the state in the volume.** Breaks ADR-0030's "nothing is left from one call to
  the next" and keeps ticket text in the volume.

## Consequences

- The promise to users stays the same: Kaizent's application never sees the login, and
  the login never leaves the tool's container and volume.
- Each tool whose login and state share a folder gets a wrapper like `kaizent-codex` in
  its image. A tool that keeps them apart needs none.
- The wrapper is security-relevant code: changes to it are reviewed as such, and its
  tests (with a stub tool) check that the volume holds only the login.
