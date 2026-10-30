# CLAUDE.md

Context and conventions for AI-assisted sessions on this repository.

## Project

**Kaizent** (*kaizen* + *agent*) is an agentic problem-solving tool. It ingests problems
(support tickets and bug reports first), assembles a role, skills and knowledge, proposes
a resolution, gets a human verdict and learns from it, running a PDCA cycle per
resolution. The repository was created as "omnisolve" and is being renamed; the product
name is Kaizent everywhere.

- Vision and core concepts: `docs/vision.md`
- Waves and MVP scope: `docs/roadmap.md`
- Decisions and open questions: `docs/adr/README.md`

**Current status: planning, no code yet.** Wave 1 (the MVP) is defined but not started.

## Language

- Everything committed is in **English (en-US)**: docs, code, comments, identifiers,
  commit messages and UI text (ADR-0004).
- The maintainer may talk to you in **Brazilian Portuguese**. Reply in the language the
  maintainer uses, but write all repository content in English.

## How to work here

- **Discuss before building.** For new scope or design questions, ask questions first
  (one round at a time), propose, and wait for approval before writing files.
- **Respect the waves.** Do not pull later-wave features (vector store, MCP, Movidesk,
  automatic replies, fine-tuning) into the current wave without the maintainer's
  explicit approval. Every wave must work end to end.
- **Record decisions.** Every architecturally significant decision gets an ADR in
  `docs/adr/` (format in ADR-0001) in the same change. Update the ADR index and move the
  item out of "Open decisions". Never edit an accepted ADR's decision; supersede it.
- **Keep docs in sync.** When scope or status changes, update `README.md` (status),
  `docs/roadmap.md` and this file.

## Architecture conventions (decided)

- Java 25, Maven (with the wrapper), Spring Boot 4.x, Spring AI (ADR-0005).
- Modular monolith with ports and adapters, with boundaries enforced by Spring Modulith
  (ADR-0006). The core depends only on ports: `TicketSource`, `ReplySink`,
  `SkillRepository`, `KnowledgeSource`, `ToolProvider`, `LlmGateway`, `RunStore`.
  Technologies live in adapters selected by configuration.
- All model access goes through `LlmGateway`. Never call a provider SDK from the core.
  Record tokens and cost for every run, and respect the per-run budget (ADR-0007).
- API-first REST with OpenAPI. The console is a React + TypeScript (Vite) SPA, bundled
  into the Spring Boot artifact for local runs (ADR-0008).
- PostgreSQL with Flyway; Testcontainers for tests; `docker compose` locally (ADR-0009).
- Skills use the Agent Skills format (`SKILL.md`) in git; git history is the skill
  version (ADR-0010).
- Knowledge in the MVP is Markdown in git, accessed through `search_knowledge` and
  `read_document` tools (ADR-0011).
- Every proposal needs a human verdict in the MVP. Autonomy levels (Junior, Mid-level,
  Senior, Specialist) are computed per subject but do not act until wave 4 (ADR-0012).
- Demo domain: Spring PetClinic, with synthetic data only (ADR-0013).
- Base package and groupId: `br.com.refatorado.kaizent`.

## Open-source hygiene

- License: Apache-2.0. Contributions are signed off under the DCO (`git commit -s`)
  (ADR-0002).
- **Never commit secrets**: API keys come from environment variables or a local `.env`
  file (git-ignored). Document variables in `.env.example` without values.
- Never commit real customer or personal data. Demo data is synthetic.
- Only add dependencies whose licenses are compatible with Apache-2.0.
- Keep the project runnable by a newcomer by following the README alone.
