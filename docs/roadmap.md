# Roadmap

Kaizent evolves in **waves**. Each wave delivers something that works end to end and adds
capabilities on top of the previous one. Wave 1, the MVP, is described in detail. Later
waves are kept at a high level and will be refined when they get closer.

| Wave | Theme | Status |
|---|---|---|
| 1 | MVP: minimal PDCA cycle with a human in the loop | Planned |
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
| **Triage (Plan)** | The LLM classifies the problem's subject and type and selects a **role** from file-based role definitions (persona, allowed skills, knowledge sources). |
| **Context assembly (Plan)** | Skills in the Agent Skills format (`SKILL.md`) are loaded from a git folder. Knowledge comes from Markdown files in git, exposed to the agent as tools (`search_knowledge` for text search, `read_document`). There is no vector store yet. |
| **Resolution (Do)** | An agent loop with tool calling (Spring AI). It produces structured output: diagnosis, suggested reply, confidence, cited sources and skills used. |
| **Review (Check)** | A web console (React SPA) over a REST API, with a queue of runs and a full trace for each one (role, skills, sources, tool calls, tokens, cost, confidence). The reviewer can **approve**, **edit and approve**, or **reject with a comment**. |
| **Reply** | A file-based outbound connector writes the approved reply to an `outbox/` folder. |
| **Learning (Act)** | On an edit or a rejection, the agent proposes a **lesson** (a new Markdown knowledge entry). If a human approves it, it is added to the knowledge base. |
| **Metrics** | Per subject: approved-as-is, approved-with-edits and rejected rates. The **autonomy level is computed and displayed, but does not act yet**: every answer still requires human approval. |
| **Data capture** | Each run stores input, assembled context, proposal, final answer, verdict, tokens and cost. This is the seed for evaluation and future fine-tuning. |
| **Cost control** | Tokens and cost per run are visible in the console, with a configurable budget limit per run. |
| **LLM providers** | Anthropic, OpenAI, Gemini or a local model (Ollama), selected by configuration behind the `LlmGateway` port. The MVP is validated with at least one cloud provider. |
| **Project hygiene** | `LICENSE`, `CONTRIBUTING.md` (DCO), `SECURITY.md`, `.env.example`, no secrets committed, and CI that builds and tests every pull request. |

### Out of scope for the MVP

Real help desk connectors (Movidesk), MCP tools, business or analytical questions, a
vector store, automatic replies, skill improvement proposals, code patches or PRs,
authentication or multi-user support, and Kubernetes deployment.

### Definition of done

A newcomer can:

1. clone the repository and set one LLM API key in a local `.env` file;
2. start the stack locally (PostgreSQL through `docker compose`, then the application);
3. drop a demo ticket into `inbox/`;
4. watch the run in the console and inspect its full trace;
5. approve or edit the proposal and see the reply file appear in `outbox/`;
6. see the subject's metrics update and, after an edit, approve a proposed lesson that
   lands in the knowledge base;
7. do all of this by following the README alone.

---

## Wave 2: real connectors, MCP tools and skill evolution

- Movidesk inbound and outbound adapter (the first real channel).
- MCP client support: MCP servers configured per role and exposed as agent tools.
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

## Wave 5: operations, Kubernetes and incident handling

- Helm chart and Kubernetes deployment (on-premise first).
- Observability with OpenTelemetry (traces, metrics, LLM usage).
- Authentication, multiple users and roles for reviewers.
- A third problem type: operational incidents (alerts, failing workloads), with MCP
  servers for infrastructure.
- Code patches and pull requests as a resolution type for bugs.

## Wave 6: fine-tuning

- Export of curated datasets from human-verified runs.
- A fine-tuning pipeline for the supported providers and local models.
- Model comparison with the wave 4 evaluation harness before anything is promoted.
