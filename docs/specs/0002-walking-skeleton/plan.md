# Plan 0002: Walking skeleton

Build on `docs/architecture.md`. This plan introduces the first, minimal version of
several modules. Later specs extend them without changing the shapes introduced here,
except where noted.

## Modules and components

| Module | Component | Responsibility |
|---|---|---|
| `workspace` | `WorkspaceProperties`, `Workspace` | Resolves `kaizent.workspace.path` and the `inbox/` and `outbox/` paths |
| `intake` | `TicketSource` (port), `FileInboxTicketSource` (adapter), `IntakePoller` | Lists `inbox/*.md` and parses the front matter minimally (`id` defaults to the file name, `title` defaults to the first line); `@Scheduled` polling creates `Problem`s and `Run`s idempotently |
| `run` | `Run`, `RunStatus`, `RunRepository` (Spring Data JDBC), `RunWorker`, `RunOrchestrator` | Claims `RECEIVED` runs (`FOR UPDATE SKIP LOCKED`), calls resolution, stores the proposal and handles failure |
| `llm` | `LlmGateway` (port), `LlmRequest<T>`, `LlmResponse<T>`, `SpringAiLlmGateway` (`gemini-api` only), `FakeLlmGateway` | Structured call; the provider is chosen by `kaizent.llm.profiles.resolution.adapter` |
| `resolution` | `SkeletonResolver` | Builds one prompt (fixed role persona and the ticket) and asks `LlmGateway` for a `Proposal`. It is replaced by `AgentRuntime` in spec 0008. |
| `review` | `ReviewService` | Validates the state and records an `APPROVED` verdict |
| `delivery` | `ReplySink` (port), `FileOutboxReplySink` | Writes `outbox/<ticket-id>.reply.md` |
| `api` | `RunsController` | Implements the generated `RunsApi` |
| console | `RunsPage`, `RunDetailPage` | Table with 5-second polling; detail view with an Approve button |

## Data

`V2__problem_and_run.sql`:

- `problem(id uuid pk, source text, external_key text, title text, body text, requester
  text, channel text, created_at timestamptz, received_at timestamptz, labels text[],
  content_hash text, attributes jsonb, unique(source, external_key))`
- `run(id uuid pk, problem_id uuid fk, status text, failure_reason text, failure_message
  text, proposal jsonb, verdict jsonb, created_at, updated_at, version int)`
- an index on `run(status, created_at)`.

## API

Added to `api/openapi.yaml`:

- `GET /runs` (paged): `RunSummary { id, ticketKey, title, status, createdAt }`;
- `GET /runs/{id}`: `RunDetail { id, status, problem, proposal?, verdict?, failure? }`;
- `POST /runs/{id}/verdict`: `VerdictRequest { type }` (only `APPROVED` is accepted in
  this spec); `409` when the state is wrong.

## Configuration

```yaml
kaizent:
  workspace.path: examples/petclinic
  intake.poll-interval: 10s
  llm.profiles.resolution: { adapter: gemini-api, model: <free-tier model id> }
spring.ai.google.genai.api-key: ${GEMINI_API_KEY:}
```

The test profile sets `adapter: fake`.

## Testing

| AC | Test |
|---|---|
| AC-1, AC-2 | Integration test with a temporary workspace: poll twice and restart the context; one run |
| AC-3, AC-4 | Orchestrator tests with `FakeLlmGateway` (success and exception) |
| AC-5 to AC-7 | API tests (MockMvc) |
| AC-8 | Vitest tests for the pages with a mocked API |
| AC-9 | End-to-end `@SpringBootTest`: file → poll → worker → verdict → outbox file |
| AC-10 | Manual run, recorded in the PR description |

## Risks and mitigations

- The Spring AI Google GenAI starter configuration may differ between versions → isolate
  it in `SpringAiLlmGateway` configuration; spec 0004 generalizes it.
- Free-tier rate limits → one call per run; spec 0004 adds retries.
