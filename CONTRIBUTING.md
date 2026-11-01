# Contributing to Kaizent

Thank you for your interest in Kaizent. This guide explains how changes are proposed,
built and reviewed. By taking part, you agree to follow the
[Code of Conduct](CODE_OF_CONDUCT.md).

## Before you start

- Read the [vision](docs/vision.md), the [roadmap](docs/roadmap.md) and the
  [wave 1 architecture](docs/architecture.md).
- For a bug, open an issue with the **Bug report** template. For an idea, use the
  **Feature request** template. Security problems go through [SECURITY.md](SECURITY.md),
  never through public issues.
- For anything bigger than a small fix, open or comment on an issue first, so that the
  approach can be agreed before you write code.

## Spec-driven development

Kaizent uses spec-driven development
([ADR-0015](docs/adr/0015-spec-driven-development.md)); the process is described in
[docs/specs/README.md](docs/specs/README.md).

- **No implementation without an `Approved` spec.** New scope starts as a spec proposal
  (`spec.md`, `plan.md`, `tasks.md`, from [the templates](docs/specs/_templates/)).
- **Reference the spec and tasks** in the pull request, for example
  `Implements 0005 T3–T5`, and tick the tasks in `tasks.md` in the same pull request.
- **Keep specs true.** If the implementation shows that a spec is wrong, update it in the
  same pull request. A significant change in scope needs the maintainer's approval first.
- **Record decisions.** An architecturally significant decision gets an ADR in
  [docs/adr/](docs/adr/README.md). Accepted ADRs are never edited; they are superseded.
- **Respect the waves.** Features from later waves of the roadmap are not pulled into the
  current one without the maintainer's approval.

## Development setup

You need **JDK 25** and **Docker**. Node.js is not required: the Maven build downloads
Node 24 for the console. Follow [Getting started](README.md#getting-started) in the
README to run the application.

```bash
./mvnw verify
```

`verify` builds the console and the server and runs every check that CI runs:

- Java formatting (Spotless with google-java-format): fix it with `./mvnw spotless:apply`;
- console linting and formatting (ESLint and Prettier): fix it with `npm run format` in
  `kaizent-console/`;
- the console tests (Vitest) and the server tests (JUnit, with Testcontainers PostgreSQL);
- the Spring Modulith module boundaries.

To work on the console with live reload, start the server and then run `npm run dev` in
`kaizent-console/` (Node 24 is required for this; the Maven build installs one in
`kaizent-console/node/`). The dev server proxies `/api` to the running application.

### Conventions

- Everything committed is in **English (en-US)**: code, comments, docs, commit messages
  and UI text ([ADR-0004](docs/adr/0004-language-policy.md)).
- The REST API is **contract-first**: change `api/openapi.yaml` first; the server
  interfaces and the console types are generated from it
  ([ADR-0024](docs/adr/0024-contract-first-rest-api.md)).
- Tests never call real LLMs; use the fake adapters
  ([ADR-0020](docs/adr/0020-testing-and-evaluation-strategy.md)).
- **Never commit secrets.** Keys and passwords come from environment variables or a local
  `.env` file (git-ignored); `.env.example` documents the variables without values.
- Demo data is synthetic. Never commit real customer or personal data.
- New dependencies must have a license compatible with Apache-2.0.

## Commits and the DCO

Kaizent uses the [Developer Certificate of Origin](https://developercertificate.org/)
(DCO) instead of a CLA ([ADR-0002](docs/adr/0002-license-apache-2-0-with-dco.md)). Every
commit must be signed off by its author:

```bash
git commit -s -m "Describe the change"
```

This adds a `Signed-off-by: Your Name <your@email>` line that must match the commit
author. The DCO check fails on pull requests with commits that are not signed off. To fix
the last commit, run `git commit --amend -s --no-edit`; for earlier commits, run
`git rebase --signoff main` and push with `--force-with-lease`.

## Pull requests

1. Fork the repository (or create a branch if you have access) from `main`.
2. Keep each pull request small and focused, ideally one slice of a spec.
3. Fill in the pull request template: what changed, the spec and task IDs, and how you
   verified it.
4. Make sure `./mvnw verify` passes locally. CI runs the same build, and the DCO check
   must pass.
5. Pull requests are merged with **Squash and merge** by the maintainer.

## License

By contributing, you agree that your contributions are licensed under the
[Apache License 2.0](LICENSE).
