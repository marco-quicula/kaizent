# AGENTS.md

Context and conventions for AI coding agents (Codex, Claude Code and others) working on
this repository. This is the single source: `CLAUDE.md` only imports this file.

## Project

**Kaizent** (*kaizen* + *agent*) is an agentic problem-solving tool. It ingests problems
(support tickets and bug reports first), assembles a role, skills and knowledge, proposes
a resolution, gets a human verdict and learns from it, running a PDCA cycle per
resolution. The repository is `marco-quicula/kaizent` (it was created as "omnisolve").

- Vision and core concepts: `docs/vision.md`
- Waves and MVP scope: `docs/roadmap.md`
- Wave 1 technical picture: `docs/architecture.md`
- Specs (process and wave 1 specs): `docs/specs/README.md`
- Decisions and open questions: `docs/adr/README.md`

**Current status: wave 1 in progress.** Wave 1 (the MVP) is broken down into specs
0001–0012, all `Approved` (2026-11-01). Specs 0001 (project foundation), 0002 (walking
skeleton) and 0003 (demo workspace and evaluation runner) are `Done`; spec 0004 (LLM
access) is next.
Progress is tracked in GitHub issues under the "Wave 1 – MVP" milestone.

## Language

- Everything committed is in **English (en-US)**: docs, code, comments, identifiers,
  commit messages and UI text (ADR-0004).
- The maintainer may talk to you in **Brazilian Portuguese**. Reply in the language the
  maintainer uses, but write all repository content in English.

## How to work here

- **Discuss before building.** For new scope or design questions, ask questions first
  (one round at a time), propose, and wait for approval before writing files.
- **Explicit approval only.** Treat a proposal as approved only when the maintainer says
  so explicitly (for example, "ok"). Silence or omission is not approval.
- **Spec-driven development (ADR-0015).** Never implement without an `Approved` spec.
  Implementation PRs reference the spec and task IDs (for example, `Implements 0005
  T3–T5`), tick the tasks in `tasks.md`, and update the spec in the same PR if the
  implementation shows that it was wrong. Templates are in `docs/specs/_templates/`.
- **Respect the waves.** Do not pull later-wave features (vector store, MCP, Movidesk,
  automatic replies, fine-tuning, Kubernetes) into the current wave without the
  maintainer's explicit approval. Every wave must work end to end.
- **Record decisions.** Every architecturally significant decision gets an ADR in
  `docs/adr/` (format in ADR-0001) in the same change. Update the ADR index and move the
  item out of "Open decisions". Never edit an accepted ADR's decision; supersede it.
- **Keep docs in sync.** When scope or status changes, update `README.md` (status),
  `docs/roadmap.md`, `docs/specs/README.md` (spec statuses) and this file (`AGENTS.md`).

## Architecture conventions (decided)

- Java 25, Maven (with the wrapper), Spring Boot 4.x, Spring AI; the latest mutually
  compatible GA versions are pinned in the parent POM (ADR-0005, ADR-0018).
- Layout: `kaizent-server` (a single Maven module, with Spring Modulith modules as
  top-level packages), `kaizent-console` (a React SPA packaged as a resources jar),
  `api/openapi.yaml` and `examples/petclinic/` (ADR-0019).
- Ports and adapters with boundaries enforced by Spring Modulith (ADR-0006). The core
  depends only on ports: `TicketSource`, `ReplySink`, `SkillRepository`,
  `KnowledgeSource`, `KnowledgeWriter`, `RoleCatalog`, `LlmGateway`, `AgentRuntime`,
  `CliExecutor`, `RunStore`. See `docs/architecture.md`.
- **LLM access (ADR-0016):** single structured calls go through `LlmGateway`; the
  resolution loop goes through `AgentRuntime`. Adapters are API (Spring AI), CLI (Codex,
  Claude Code, Antigravity, Gemini CLI presets, through `CliExecutor`) or fake, selected
  per pipeline step by configuration. Never call a provider SDK or a CLI from the core.
  CLI tools always run in a new container per call, with only the granted workspace paths
  mounted read-only (never `evals/`) and a timeout (ADR-0030). Record usage and the cost type for every call, and respect
  the per-run budget.
- The pipeline is orchestrated in our own code; only the resolution step is an agent loop
  (ADR-0017). Runs follow the state machine of ADR-0023.
- Prompts are versioned resource files in `kaizent-server/src/main/resources/prompts/`,
  in English. The run records each prompt's content hash.
- Contract-first REST: change `api/openapi.yaml` first, then generate the code (ADR-0024).
  Console tooling: ADR-0025.
- PostgreSQL 18 with Flyway and Spring Data JDBC; Testcontainers for tests; `docker
  compose` locally (ADR-0009, ADR-0023).
- Skills use the Agent Skills format (`SKILL.md`) in git; git history is the skill
  version (ADR-0010). Knowledge is Markdown searched with in-memory Lucene (ADR-0011,
  ADR-0021). Workspace layout: ADR-0027. Kaizent never runs git commands in wave 1.
- Every proposal needs a human verdict in the MVP. Autonomy levels (Junior, Mid-level,
  Senior, Specialist) are computed per subject but have no effect until wave 4 (ADR-0012).
- **Testing (ADR-0020):** CI never calls real LLMs. Use the fake adapters and fake CLI
  scripts. LLM-dependent behavior is checked with opt-in evaluation cases.
- Demo domain: Spring PetClinic, with synthetic data only (ADR-0013).
- Base package and groupId: `br.com.refatorado.kaizent`.

## Open-source hygiene

- License: Apache-2.0. Contributions are signed off under the DCO (`git commit -s`)
  (ADR-0002).
- **Commit identity:** commits made for the maintainer must have the maintainer as
  author and committer, with a matching DCO sign-off. Before committing, set
  `git config user.name "Marco Quiçula"` and
  `git config user.email "marco.quicula@quicula.com.br"`, then **always** use
  `git commit -s`. Check the author, committer and `Signed-off-by` before pushing. Never
  sign off in the name of an AI assistant. Cloud sessions sign commits with the
  session's own SSH key, which is not registered on the maintainer's GitHub account, so
  branch commits show as "Unverified". This is expected: pull requests are merged with
  **Squash and merge**, so the commit on `main` is signed by GitHub and shows as
  "Verified". Never ask for or store the maintainer's private keys.
- **Never commit secrets**: API keys come from environment variables or a local `.env`
  file (git-ignored). Document variables in `.env.example` without values. Never read,
  copy or log CLI tools' credential files.
- Never commit real customer or personal data. Demo data is synthetic.
- Only add dependencies whose licenses are compatible with Apache-2.0.
- Keep the project runnable by a newcomer by following the README alone.
