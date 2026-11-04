# Architecture (wave 1)

This document is the shared technical picture for the wave 1 specs. The reasons behind
each choice are in the [ADRs](adr/README.md). Each spec's `plan.md` refers to the
sections below instead of repeating them.

## 1. Big picture

```
                 ┌──────────────────────────── kaizent-server (Spring Boot) ─────────────────────────────┐
 workspace/      │                                                                                        │
 inbox/*.md ───► │ intake ──► run (orchestrator, state machine, worker) ──► delivery ───────────────────► │ ──► outbox/*.md
                 │              │        │            │                                                  │
                 │              ▼        ▼            ▼                                                  │
                 │           triage   resolution   review ──(VerdictRecorded)──► learning ──► knowledge/lessons/
                 │              │        │  ▲                      │                                       │
                 │              ▼        ▼  │                      └──► metrics                            │
                 │           llm ◄───── agent runtime ──► knowledge (Lucene) / skills                      │
                 │                                                                                        │
                 │ api (REST /api/v1 + SSE) ◄─── kaizent-console (React SPA served at /)                   │
                 └───────────────────────────────────────────┬────────────────────────────────────────────┘
                                                             ▼
                                                        PostgreSQL
```

## 2. Application modules

Each module is a top-level package under `br.com.refatorado.kaizent`, and Spring
Modulith verifies the dependencies between them. Inside a module:

- the module's root package holds its **public API**: ports, domain types and events
  exposed to other modules;
- `internal/` holds application services and domain logic;
- `adapter/<technology>/` holds adapters.

| Module | Responsibility | Ports it owns | Wave 1 adapters |
|---|---|---|---|
| `workspace` | Loads and validates the workspace layout and `kaizent-workspace.yaml` | `WorkspaceConfig` | file system |
| `intake` | Reads tickets, maps them to `Problem`, records intake errors | `TicketSource` | `adapter.file` (inbox polling) |
| `run` | The `Run` aggregate, state machine, worker, orchestrator, trace, budgets | `RunStore` | `adapter.jdbc` |
| `llm` | Single structured model calls; usage and pricing | `LlmGateway`, `CliExecutor` | `adapter.springai`, `adapter.cli`, `adapter.fake` |
| `triage` | Classifies subject, problem type and language; selects the role | `RoleCatalog` | `adapter.file` (roles/) |
| `skills` | Skill catalog and content, with versions | `SkillRepository` | `adapter.file` |
| `knowledge` | Search and read the knowledge base; write lessons | `KnowledgeSource`, `KnowledgeWriter` | `adapter.lucene`, `adapter.file` |
| `resolution` | Builds the agent task, runs it, validates the proposal, computes confidence | `AgentRuntime` | `adapter.native` (Spring AI loop), `adapter.cli`, `adapter.fake` |
| `review` | Records verdicts | n/a | n/a |
| `delivery` | Sends approved replies | `ReplySink` | `adapter.file` (outbox) |
| `learning` | Drafts lessons after edits or rejections; approves or discards them | n/a | n/a |
| `metrics` | Per-subject metrics and autonomy levels | n/a | `adapter.jdbc` (queries) |
| `evaluation` | Runs evaluation cases against real adapters (`eval` profile) and writes reports | n/a | n/a |
| `api` | REST controllers implementing the generated interfaces; SSE stream | n/a | n/a |

Allowed dependencies follow the pipeline. For example, `run` depends on `intake`,
`triage`, `resolution` and `delivery` through their ports. `api` depends on every
module's public API. No module depends on `api`.

## 3. Domain model (wave 1)

| Type | Key fields |
|---|---|
| `Problem` | `id`, `source`, `externalKey`, `title`, `body`, `requester`, `channel`, `createdAt`, `receivedAt`, `labels`, `contentHash`, `attributes` (ADR-0028) |
| `Subject` | `id` (slug), `name`, `description`; declared in `kaizent-workspace.yaml` |
| `Role` | `id`, `name`, `subjects`, `allowedSkills`, `knowledgePaths`, `persona` (Markdown body) |
| `Skill` | `name`, `description`, `path`, `version` (commit or content hash), `content` (loaded on demand) |
| `Run` | `id`, `problemId`, `status`, `failureReason`, `triage`, `roleId`, `proposal`, `verdict`, `delivery`, `usage`, `budget`, timestamps |
| `TriageResult` | `subjectId` (or `unknown`), `problemType` (`BUG`, `HOW_TO`, `OTHER`), `language`, `rationale` |
| `Proposal` | `diagnosis`, `suggestedReply`, `rawConfidence`, `adjustedConfidence`, `confidenceRationale`, `appliedRules`, `citations[]` (`path`, `section`), `skillsUsed[]` (`name`, `version`), `lacksInformation` |
| `Verdict` | `type` (`APPROVED`, `APPROVED_WITH_EDITS`, `REJECTED`), `finalReply`, `comment`, `reviewer`, `reviewedAt` |
| `TraceEvent` | `runId`, `sequence`, `type`, `step`, `summary`, `details` (JSON), `usage`, `timestamp` |
| `Usage` | `inputTokens`, `outputTokens`, `cost`, `costType` (`API`, `SUBSCRIPTION`, `UNKNOWN`), `adapter`, `model` |
| `Lesson` | `id`, `runId`, `subjectId`, `title`, `content`, `status` (`PROPOSED`, `APPROVED`, `DISCARDED`), `path` |
| `IntakeIssue` | `source`, `externalKey`, `severity` (`ERROR`, `WARNING`), `message`, `seenAt` |

## 4. Run state machine

See ADR-0023.

```
RECEIVED → TRIAGING → RESOLVING → AWAITING_REVIEW ─┬→ APPROVED → DELIVERED
                                                   └→ REJECTED
TRIAGING | RESOLVING | APPROVED ─→ FAILED (ERROR | BUDGET_EXCEEDED | TIMEOUT)
FAILED ─(retry)→ RECEIVED
RESOLVING ─(review mode SKIP, evaluation only)→ EVALUATED
```

`APPROVED → DELIVERED` can fail (for example, the outbox is not writable). The run then
goes to `FAILED`, and a retry resumes at delivery without asking for a new verdict.

## 5. Pipeline steps

1. **Intake** (`intake`): poll `inbox/`, then map each file to a `Problem` (or an
   `IntakeIssue`), then create a `Run` in `RECEIVED`.
2. **Triage** (`triage` via `LlmGateway`, profile `triage`): the subjects list and the
   problem go in, `TriageResult` comes out. The role is chosen deterministically: the
   role whose `subjects` contain the subject, otherwise the workspace's default role.
3. **Context assembly** (`resolution`): role persona, problem, skills catalog (names and
   descriptions of the role's allowed skills), knowledge roots and output schema.
4. **Resolution** (`resolution` via `AgentRuntime`, profile `resolution`): the agent
   reads skills and knowledge as needed and returns a `Proposal`. Kaizent validates the
   schema and the citations and computes the adjusted confidence (ADR-0022).
5. **Review** (`review`): a human gives a verdict in the console, and the
   `VerdictRecorded` event is published.
6. **Delivery** (`delivery`): an approved reply goes to `outbox/` (ADR-0028).
7. **Learning** (`learning`, on `VerdictRecorded` with edits or rejection, via
   `LlmGateway`, profile `lesson`): a `Lesson` is drafted for human approval and, once
   approved, written by `KnowledgeWriter` and re-indexed.
8. **Metrics** (`metrics`): computed from runs and verdicts on read.

## 6. LLM access

See ADR-0016.

```
LlmGateway.complete(LlmRequest<T>) → LlmResponse<T>        # one structured call
AgentRuntime.run(AgentTask, TraceSink) → AgentResult       # one agentic task
```

- **Profiles** map pipeline steps to adapters, in `application.yaml` or environment
  variables:

```yaml
kaizent:
  llm:
    profiles:
      triage:     { adapter: gemini-api, model: <model-id> }
      lesson:     { adapter: gemini-api, model: <model-id> }
  agent:
    resolution:   { runtime: cli, preset: codex, timeout: 5m }
```

- **API adapters** (Spring AI): `gemini-api`, `openai`, `anthropic`, `ollama`. The
  native `AgentRuntime` exposes the tools `search_knowledge(query, limit)`,
  `read_document(path, section?)` and `read_skill(name)`.
- **CLI adapters**: presets `codex` (validated in wave 1), `claude-code`, `antigravity`
  and `gemini-cli` (provided as configuration examples, validated when someone has
  access), plus `generic` (command template). Commands run through `CliExecutor` →
  `DockerCliExecutor`: a new container per call, with only the granted workspace paths
  (the role's knowledge and skills) mounted read-only, the tool's credential volume and a
  mandatory timeout (ADR-0030).
- **Fake adapters** (`fake`) return scripted responses from test resources.
- **Pricing**: `kaizent.llm.pricing.<model>.input-per-million` and `output-per-million`.
  A missing price gives `costType = UNKNOWN` for API adapters.

## 7. Persistence

PostgreSQL 18, Flyway and Spring Data JDBC (ADR-0009, ADR-0023).

| Table | Purpose |
|---|---|
| `problem` | Canonical problems; unique `(source, external_key)` |
| `run` | Run aggregate with status, triage, role, proposal (JSONB), verdict (JSONB), usage totals, budget |
| `trace_event` | Ordered events per run (JSONB `details`) |
| `intake_issue` | Intake errors and warnings |
| `lesson` | Lesson drafts and decisions |
| `event_publication` | Spring Modulith event publication registry |

## 8. REST API (`/api/v1`)

The contract is `api/openapi.yaml` (ADR-0024). Wave 1 endpoints:

| Method and path | Purpose |
|---|---|
| `GET /runs?status=&subject=&page=&size=` | Run queue |
| `GET /runs/{id}` | Run detail (problem, triage, role, proposal, verdict, usage) |
| `GET /runs/{id}/trace` | Trace events |
| `POST /runs/{id}/verdict` | Record a verdict `{type, finalReply?, comment?}` |
| `POST /runs/{id}/retry` | Retry a failed run |
| `GET /events` | SSE stream of run and lesson changes |
| `GET /knowledge/document?path=&section=` | Read a cited knowledge document |
| `GET /lessons?status=` | Lesson drafts |
| `POST /lessons/{id}/approve` | Approve `{title?, content?}`, which writes the file |
| `POST /lessons/{id}/discard` | Discard `{comment?}` |
| `GET /subjects` | Subjects with metrics and autonomy level |
| `GET /intake-issues` | Intake errors and warnings |
| `GET /system` | Workspace, active adapters per profile, budgets, version |

## 9. Configuration and secrets

- `kaizent.workspace.path`: the workspace folder (default `examples/petclinic`).
- `kaizent.intake.poll-interval`: inbox polling interval (default 10 seconds).
- `kaizent.budget.*`: per-run limits (tokens, cost, tool calls, timeout).
- `kaizent.confidence.*`: confidence caps (ADR-0022).
- `kaizent.autonomy.*`: provisional autonomy thresholds (spec 0012).
- Secrets come only from environment variables or a `.env` file (git-ignored), for
  example `GEMINI_API_KEY` or `OPENAI_API_KEY`. `.env.example` lists them without values.

## 10. Security notes

- The console has no authentication in wave 1 and binds to `localhost` by default.
- CLI tools run read-only, in the workspace, with timeouts (ADR-0016).
- Paths received from the API or from agents (citations, `read_document`) are normalized
  and must resolve inside the workspace. Anything else is rejected.
- No ticket content is logged at INFO level or above. The trace in the database is the
  audit record.
