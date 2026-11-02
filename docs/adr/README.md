# Architecture Decision Records

This folder records the significant decisions about Kaizent, together with the reasons
behind them. The format is described in [ADR-0001](0001-record-architecture-decisions.md).

## Decisions

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions as ADRs | Accepted |
| [0002](0002-license-apache-2-0-with-dco.md) | License under Apache-2.0, with contributions under the DCO | Accepted |
| [0003](0003-project-name-and-coordinates.md) | Name: Kaizent; coordinates `br.com.refatorado.kaizent` | Accepted |
| [0004](0004-language-policy.md) | English for all repository content | Accepted |
| [0005](0005-backend-stack-java-spring.md) | Java 25, Maven, Spring Boot 4, Spring AI | Accepted |
| [0006](0006-modular-monolith-hexagonal.md) | Modular monolith with ports and adapters (Spring Modulith) | Accepted |
| [0007](0007-llm-gateway-provider-neutral.md) | Provider-neutral LLM access with cost tracking | Superseded by 0016 |
| [0008](0008-api-first-react-console.md) | API-first backend with a React + TypeScript SPA console | Accepted |
| [0009](0009-postgresql-persistence.md) | PostgreSQL (Flyway) for operational data; pgvector later | Accepted |
| [0010](0010-skills-agent-skills-format-in-git.md) | Skills in the Agent Skills format, versioned in git | Accepted |
| [0011](0011-markdown-knowledge-before-vectors.md) | Markdown knowledge in git before vector retrieval | Accepted |
| [0012](0012-human-in-the-loop-and-autonomy-levels.md) | Human in the loop; autonomy levels earned per subject | Accepted |
| [0013](0013-demo-domain-spring-petclinic.md) | Spring PetClinic as the demo domain, with synthetic data | Accepted |
| [0014](0014-file-based-connectors-for-mvp.md) | File-based connectors and a canonical problem model for the MVP | Accepted |
| [0015](0015-spec-driven-development.md) | Spec-driven development (spec, plan and tasks per slice) | Accepted |
| [0016](0016-llm-access-api-and-cli-adapters.md) | LLM access through `LlmGateway` and `AgentRuntime`, with API, CLI and fake adapters | Accepted |
| [0017](0017-pipeline-orchestration-in-own-code.md) | Pipeline orchestration in our own code on Spring AI (Embabel deferred) | Accepted |
| [0018](0018-dependency-version-baseline.md) | Dependency version baseline and pinning policy | Accepted |
| [0019](0019-repository-and-module-layout.md) | Repository and Maven module layout | Accepted |
| [0020](0020-testing-and-evaluation-strategy.md) | Testing without real LLMs in CI; opt-in evaluations | Accepted |
| [0021](0021-knowledge-search-with-lucene.md) | Knowledge search with an in-memory Lucene index | Accepted |
| [0022](0022-confidence-scoring.md) | Confidence: self-reported, capped by evidence rules | Accepted |
| [0023](0023-run-execution-model.md) | Run execution model: Postgres queue, single worker, state machine | Accepted |
| [0024](0024-contract-first-rest-api.md) | Contract-first REST API with OpenAPI | Accepted |
| [0025](0025-console-tooling.md) | Console tooling: Node 24, Mantine, TanStack Query, Vitest | Accepted |
| [0026](0026-ci-and-repository-automation.md) | CI and repository automation (GitHub Actions, DCO, Dependabot) | Accepted |
| [0027](0027-workspace-layout.md) | Workspace layout; Kaizent never runs git commands in wave 1 | Accepted |
| [0028](0028-ticket-file-format-and-canonical-problem.md) | Ticket file format and canonical problem model | Accepted |
| [0029](0029-reply-language-follows-ticket.md) | Replies use the ticket's language | Accepted |

## Open decisions

These questions are known but not decided yet. Each one becomes an ADR when it is
resolved. The "Needed by" column says when the answer is required.

| # | Question | Needed by | Notes |
|---|---|---|---|
| 1 | Redaction of personal data before prompts leave the system, and before traces are stored | Before the first real connector (wave 2) | |
| 2 | Kaizent as an MCP server: exposing its tools to CLI runtimes and other clients | Wave 2 | Required before the vector store (wave 3). |
| 3 | Delivery of skill improvement proposals: branch, pull request, or both; which git hosting APIs; Kaizent's git identity | Wave 2 | ADR-0027 defers git writes to wave 2. |
| 4 | Embedding model(s) and the chunking strategy | Wave 3 | Local ONNX option vs. cloud APIs. |
| 5 | Final thresholds and minimum sample sizes for the autonomy levels, and how levels act | Wave 4 | Provisional values in spec 0012. |
| 6 | Agent runner service for Kubernetes (`RemoteRunnerExecutor`): API, isolation, credential handling, terms-of-use guidance | Wave 5 | Seam already in ADR-0016. |
| 7 | Authentication and roles for the console | Wave 5 | |
| 8 | Trace retention and purge policy | Wave 5 | |
| 9 | Trademark search and registration for "Kaizent" | Before public promotion | |
| 10 | Workspace bootstrap and guided setup for a new deployment: a template, an `init` command, a validation report and a step-by-step guide | Not scheduled | The most important of these for adoption; needed at the latest by the first real deployment. |
| 11 | Importing existing knowledge (for example Confluence, Notion, SharePoint, PDFs): one-off conversion to Markdown, an MCP knowledge source, or continuous sync | Not scheduled | Relates to MCP knowledge sources (wave 2) and to 16. |
| 12 | Editing roles, skills and knowledge in the console, or in git only; how console edits are reviewed and versioned | Not scheduled | Relates to 3 (Kaizent's git identity) and to 16. |
| 13 | Several workspaces in one instance: isolation of data, configuration, budgets and console views | Not scheduled | Today an instance serves one workspace (`kaizent.workspace.path`). Relates to 16. |
| 14 | Several instances of the same agent on one workspace: distributing intake and work, scheduled tasks that must run once, shared or per-instance workspace files, knowledge index consistency, global LLM rate limits and budgets | Not scheduled | ADR-0023 already makes the run claim safe across instances (`FOR UPDATE SKIP LOCKED`). Kubernetes deployment is planned in wave 5. Relates to 16. |
| 15 | Domain-agnostic problem types and prompt framing: problem types defined per workspace instead of the fixed `BUG`, `HOW_TO` and `OTHER`, and prompts that do not assume support tickets | Not scheduled | Affects triage (spec 0006). |
| 16 | Where the workspace content lives (roles, skills, knowledge, lessons). Proposal: the source of truth is a per-workspace setting. With **git** as the source, PostgreSQL holds a derived copy, synced one way whenever the repository changes, with the commit as the version. With the **database** as the source, content is versioned in the database (revisions, hashes, approvals), and export to files in the workspace format is a feature. Open points: change detection (file watching, polling the commit, or a webhook from the git host), console edits when git is the source (blocked, or proposed as commits; see 3 and 12), and runs that are in progress during a change | Not scheduled | The ports (`RoleCatalog`, `SkillRepository`, `KnowledgeSource`, `KnowledgeWriter`) allow a database adapter without changing the core. Answers to 11–14 depend on it. Adopting the database as a source would supersede the storage parts of ADR-0010, ADR-0011 and ADR-0027. |
