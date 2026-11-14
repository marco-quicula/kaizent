# Roadmap

Kaizent evolves in **waves**. Each wave delivers something that works end to end and adds
capabilities on top of the previous one. Wave 1, the MVP, is described in detail. Later
waves are kept at a high level and will be refined when they get closer.

| Wave | Theme | Status |
|---|---|---|
| 1 | MVP: minimal PDCA cycle with a human in the loop | In progress |
| 2 | Real connectors, MCP tools and skill evolution | Planned |
| 3 | Vector-based retrieval (RAG) | Planned |
| 4 | Graduated autonomy and evaluation | Planned |
| 5 | Operations, Kubernetes and incident handling | Planned |
| 6 | Fine-tuning | Planned |

---

## Wave 1: MVP, a minimal PDCA cycle with a human in the loop

### Goal

Prove the complete cycle on one problem type (support tickets and bug reports):
ingest → triage → role → context → proposal → human review → reply → lesson → metrics.
Everything runs locally, uses plain files and is reproducible by anyone who clones the
repository.

### Demo domain

The [Spring PetClinic](https://github.com/spring-projects/spring-petclinic) sample
application plays the role of the "product being supported". The repository ships
**synthetic** demo data:

- example tickets (bug reports with stack traces and how-to questions);
- a small knowledge base in Markdown (docs, FAQ, known issues);
- a few skills and role definitions.

No real customer data is used. See [ADR-0013](adr/0013-demo-domain-spring-petclinic.md).

### Scope

| Step | Delivered in the MVP |
|---|---|
| **Ingestion** | A file-based inbound connector: a folder (`inbox/`) in a git repository, with one ticket per file (Markdown body plus YAML front matter) and polling. It maps each ticket to the canonical `Problem` model. |
| **Triage (Plan)** | The LLM classifies the problem's subject and type, and the code selects the **role** from file-based role definitions (persona, allowed skills, knowledge sources), deterministically. See [spec 0006](specs/0006-triage-and-roles/spec.md). |
| **Context assembly (Plan)** | Skills in the Agent Skills format (`SKILL.md`) are loaded from a git folder. Knowledge comes from Markdown files in git, exposed to the agent as tools (`search_knowledge` for text search, `read_document`). There is no vector store yet. |
| **Resolution (Do)** | An agent loop with tool calling (Spring AI). It produces structured output: diagnosis, suggested reply, confidence, cited sources and skills used. |
| **Review (Check)** | A web console (React SPA) over a REST API, with a queue of runs and a full trace for each one (role, skills, sources, tool calls, tokens, cost, confidence). The reviewer can **approve**, **edit and approve**, or **reject with a comment**. |
| **Reply** | A file-based outbound connector writes the approved reply to an `outbox/` folder. |
| **Learning (Act)** | On an edit or a rejection, the agent proposes a **lesson** (a new Markdown knowledge entry). If a human approves it, it is added to the knowledge base. |
| **Metrics** | Per subject: approved-as-is, approved-with-edits and rejected rates. The **autonomy level is computed and displayed, but does not act yet**: every answer still requires human approval. |
| **Data capture** | Each run stores input, assembled context, proposal, final answer, verdict, tokens and cost. This is the seed for evaluation and future fine-tuning. |
| **Cost control** | Tokens and cost per run are visible in the console, with a configurable budget limit per run. The budget is a soft ceiling, checked before every provider call (repairs and retries included), so a run can exceed it by at most one call. |
| **LLM access** | Two ports, `LlmGateway` (single structured calls) and `AgentRuntime` (the resolution loop), with **API adapters** (Gemini, OpenAI, Anthropic, Ollama, through Spring AI) and **CLI adapters** (Codex validated; Claude Code, Antigravity and Gemini CLI presets provided) selected per pipeline step by configuration. Validated with Codex CLI, the Gemini API free tier and fake adapters. See [ADR-0016](adr/0016-llm-access-api-and-cli-adapters.md). |
| **Evaluation** | An opt-in runner executes the demo's evaluation cases against real adapters and writes a report. CI never calls real models. See [ADR-0020](adr/0020-testing-and-evaluation-strategy.md). |
| **Project hygiene** | `LICENSE`, `CONTRIBUTING.md` (DCO), `SECURITY.md`, `.env.example`, no secrets committed, and CI that builds and tests every pull request. |

### Out of scope for the MVP

Real help desk connectors (Movidesk), MCP tools, business or analytical questions, a
vector store, automatic replies, skill improvement proposals, code patches or PRs,
authentication or multi-user support, and Kubernetes deployment.

### Definition of done

A newcomer can:

1. clone the repository and configure one LLM access: an API key in a local `.env` file
   (the Gemini free tier works) or a logged-in local CLI tool (Codex);
2. start the stack locally (PostgreSQL through `docker compose`, then the application);
3. drop a demo ticket into `inbox/`;
4. watch the run in the console and inspect its full trace;
5. approve or edit the proposal and see the reply file appear in `outbox/`;
6. see the subject's metrics update and, after an edit, approve a proposed lesson that
   lands in the knowledge base;
7. do all of this by following the README alone.

The closing criterion of the wave is an automated **full-cycle test** with the fake
adapters (spec 0011 AC-11): a ticket is triaged and resolved with tool calls, a reviewer
edits and approves it, the reply appears in `outbox/`, the drafted lesson is approved and
published once, and a second ticket finds the lesson and cites it.

### Specs

Wave 1 is broken down into 12 specs, listed in implementation order in
[docs/specs/README.md](specs/README.md). The shared technical picture is in
[docs/architecture.md](architecture.md).

---

## Wave 2: real connectors, MCP tools and skill evolution

- Movidesk inbound and outbound adapter (the first real channel).
- MCP client support: MCP servers configured per role and exposed as agent tools.
- Kaizent as an MCP server: its knowledge and skill tools exposed to CLI runtimes, so
  that every runtime uses the same tools and produces the same trace.
- A second problem type: business and analytical questions over internal knowledge.
- **Skill improvement proposals**, delivered as a branch or pull request on the skills
  repository, and **new-skill suggestions**.

## Wave 3: vector-based retrieval (RAG)

- PostgreSQL with pgvector as the vector store, sitting behind the knowledge-source port.
- Embedding models, with a local CPU-friendly option (ONNX) besides cloud APIs.
- Indexing of resolved runs and approved lessons; hybrid (text and vector) search.

## Wave 4: graduated autonomy and evaluation

- Autonomy levels start to act: automatic replies above a per-subject threshold, plus
  sampling audits of automated answers.
- Outcome signals from connectors (for example, the reopen rate and ratings when the
  channel provides them).
- An evaluation harness built from recorded runs, used to detect regressions when skills
  or models change.
- A PDCA dashboard per subject.
- Holding a suspicious ticket (ADR-0032) until a person decides how to resolve it: a run
  state outside the worker's queue and a decision action in the console, which changes
  the run lifecycle of ADR-0023.

## Wave 5: operations, Kubernetes and incident handling

- Helm chart and Kubernetes deployment (on-premise first).
- Agent runner: runs headless CLI tasks for Kaizent through `RemoteRunnerExecutor`, as
  one Kubernetes Job or Pod per call, with the same images and container contract as the
  wave 1 `DockerCliExecutor` (ADR-0030), plus a how-to covering isolation, credentials and
  the providers' terms of use. Usage rules, agreed with the maintainer (a documented
  policy, not a technical lock; complying with each provider's terms stays with the user):
  - a CLI tool's **subscription** login is for personal use only: the local Docker mode of
    wave 1, or a local Kubernetes (for example Rancher Desktop with k3s) used only to test
    this runner, not to run Kaizent;
  - a **shared cluster** may run agentic tasks, but **only with API access**: Kaizent's
    own loop through the API adapters (the default), or a CLI tool with an API key when an
    evaluation shows a clear gain from its agent. Single structured calls always use the
    API adapters there.

  An ADR records the credential model when this wave starts.
- Observability with OpenTelemetry (traces, metrics, LLM usage).
- Cost reconciliation (idea): a background job that compares the estimated cost of the
  model calls (tokens times `kaizent.llm.pricing`) with each provider's usage or billing
  API, to capture the real charge, cached-input discounts and free tiers. It needs admin
  credentials per provider and never runs in the resolution flow.
- Authentication, multiple users and roles for reviewers.
- A third problem type: operational incidents (alerts, failing workloads), with MCP
  servers for infrastructure.
- Code patches and pull requests as a resolution type for bugs.

## Wave 6: fine-tuning

- Export of curated datasets from human-verified runs.
- A fine-tuning pipeline for the supported providers and local models.
- Model comparison with the wave 4 evaluation harness before anything is promoted.
