# Security policy

## Supported versions

Kaizent is under active development and has no releases yet. Security fixes are made on
the `main` branch.

## Reporting a vulnerability

Please **do not open a public issue** for security problems.

Report vulnerabilities privately through GitHub: open the repository's **Security** tab
and choose **Report a vulnerability**. Include what you found, the steps to reproduce it
and the impact you expect.

You can expect an acknowledgment within a few days. The maintainer will keep you informed
of the progress and will credit you in the fix, unless you prefer to stay anonymous.

## Scope notes

- Kaizent is meant to run locally in wave 1: the server binds to `127.0.0.1` and the
  console has no authentication. Exposing it on a network is not supported yet.
- Secrets (API keys, the database password) must come from environment variables or a
  local `.env` file and must never be committed. If you find a committed secret, report it
  as described above.
- CLI tools used as LLM adapters run read-only, inside the workspace, with a timeout
  ([ADR-0016](docs/adr/0016-llm-access-api-and-cli-adapters.md)). A way around these limits
  is a security problem.
