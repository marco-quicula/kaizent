# Plan 0009: Run trace, usage and budget

## Components

| Module | Component | Responsibility |
|---|---|---|
| `run` | `TraceEvent`, `TraceEventRepository` | Persistence of events (append-only) |
| `run` | `RunTraceSink` (implements `TraceSink`) | Assigns the sequence numbers and the attempt; applies the `OutputLeakCheck` (spec 0008 AC-12) to every model-produced text and every payload, then caps it (`max-payload`), and only then writes, in the current transaction. Redaction comes **before** truncation and persistence, on every path (valid, invalid and repair responses, errors, unknown events), and each redaction becomes a flag on the event |
| `run` | `BudgetGuard` | Tracks the remaining budget (tokens, cost, tool calls, deadline) per run, or per lesson drafting with its own budget; exposed to adapters as a `CallBudget` handle (below) |
| `llm` | `TracingLlmGateway` (decorator) | Wraps every `LlmGateway`: records an `LLM_CALL` with usage and duration for each effective provider call reported by the adapter (not only one per `complete`) |
| `resolution` | `TracingAgentRuntime` (decorator) | Same for `AgentRuntime`; native tool calls report through the `TraceSink`; enforces the wall-clock timeout as a deadline on the whole run |
| `run` | `RunRetryService` | Retry rules (AC-9) |

The decorators are applied by configuration. Tracing stays outside the adapters, but the
budget cannot: a decorator around `complete` or `run` sees one call, while the adapters
make several provider calls inside it (the repair attempt and the retries of spec 0004,
and every turn of the agent loop). So the adapters receive a small handle and use it at
the one seam where they call the provider:

```java
interface CallBudget {
  // Called immediately before each provider call. Throws BudgetExceededException
  // (BUDGET_EXCEEDED) or DeadlineExceededException (TIMEOUT) without calling; otherwise
  // returns the output-token limit to send to the provider, when it accepts one.
  CallLimits beforeCall(int estimatedInputTokens);
  // Called after the call with the reported usage (unknown fields stay unknown).
  void afterCall(Usage usage);
}
```

`LlmRequest` and `AgentTask` carry the handle (`CallBudget budget`). The seam differs per
adapter: a Spring AI adapter calls the handle before each provider call it makes; the CLI
adapter (single calls) calls it before each `CliExecutor.execute`, and `afterCall` with the
aggregated usage the tool reported, which may be unknown. The tool's internal turns are not
seen, so a CLI call can overshoot by one whole call, within the container timeout. An agent
mode CLI needs a per-turn mechanism before it is released (spec 0009 AC-7). `StructuredOutputs`
(repair), `Retrying` (each attempt) and the native loop (each turn) call `beforeCall` and
`afterCall`. The lesson drafting passes a handle of its own budget (`kaizent.budget.lesson`).

**Token estimation:** a character-based estimate (characters / 4), with a configurable
safety factor. It only guards against obvious overruns; the actual usage comes from the
provider. The estimate covers the input; the output is bounded by the limit sent to the
provider (the smaller of the remaining output budget and `max-output-per-call`) and, when
a provider cannot take a limit, by the soft ceiling below.

**Soft ceiling.** Usage is known only after a call, so the budget cannot be a hard cap:
the guard stops the *next* call, and a run can overshoot a limit by at most the usage of
one call. The README and the configuration comments say so.

**Unknown usage.** When a call reports no usage (or only part of it), the totals keep it
unknown (and count the calls), while the guard debits the **conservative upper bound** of that
call from the remaining balance, per dimension: input = the estimate; output = the output
limit sent to the provider (`max-output-per-call`, or the remaining output budget if smaller);
cost = those values at the configured price, and with no price the cost dimension does not
limit and the token limits apply. The call is marked `estimated` in its event. If the
provider accepts no output limit and reports no usage, nothing bounds one call from above, so
the guarantee becomes "the next call is barred by the estimated balance". The guarantee holds
on estimates, not on real usage.

## Data

`V6__trace_and_usage.sql`:

- `trace_event(id bigserial, run_id uuid, attempt int, sequence int, type text, step
  text, summary text, details jsonb, input_tokens int, output_tokens int, cost numeric,
  cost_type text, duration_ms int, created_at timestamptz, unique(run_id, attempt,
  sequence))`;
- `run` gains `attempt int`, `usage jsonb` (totals per cost type) and `budget jsonb`.

## API

- `GET /runs/{id}/trace?attempt=` → `TraceEvent[]`.
- `POST /runs/{id}/retry` → `RunDetail`.
- `RunDetail` gains `attempt`, `usage` and `budget`.

## Configuration

```yaml
kaizent:
  budget:
    max-input-tokens: 200000
    max-output-tokens: 20000
    max-cost: 0.50          # in USD; applies to API cost only
    max-tool-calls: 20
    timeout: 10m
    max-output-per-call: 4000   # sent to the provider as the output limit, when it accepts one
    # A soft ceiling: a run can exceed a limit by at most one call (usage is known only afterwards)
    lesson: { max-input-tokens: 50000, max-output-tokens: 4000, max-cost: 0.10, timeout: 2m }  # spec 0011, separate from the run
  trace.max-payload: 256KB
```

## Testing

| AC | Test |
|---|---|
| AC-1 to AC-5 | Orchestrator tests with fakes, asserting the event sequences |
| AC-2 to AC-4 (redaction) | `RunTraceSink` tests with a synthetic token in a valid response, an invalid response, a repair request and response, an error message and an unmapped event: nothing is persisted unredacted, each case carries a flag, and redaction happens before truncation |
| AC-6 to AC-8 | `BudgetGuard` and decorator unit tests (the fake reports usage that exceeds the limits); a test where the first answer is invalid, and the repair attempt and a retry each fail with `BUDGET_EXCEEDED` **before** calling the provider (the stub counts its calls); the output limit reaches the stub provider; a call with unknown usage stays unknown in the totals; the overshoot of a run is at most one call; **several calls with no reported usage** (and a provider that takes no output limit) debit the conservative upper bound each, mark the calls `estimated`, and exhaust the estimated balance, so that the next call fails with `BUDGET_EXCEEDED` before it is made, while the reported total stays unknown and not zero; with no configured price, only the token limits stop the run |
| AC-12 | The lesson drafting runs with its own budget when the run's budget is already spent; exceeding it fails the drafting and leaves the run untouched |
| AC-9 | Retry service and API tests |
| AC-10 | API tests |
| AC-11 | A transaction test that forces a failure after the state change |

## Risks and mitigations

- Trace size grows quickly → payload caps; retention comes in a later wave.
- Storing full prompts keeps ticket content in the database → acceptable with synthetic
  data; redaction is an open decision before the first real connector. (This is about
  personal data. Secrets and token formats are redacted from model text in wave 1, by
  the output check.)
- A redacted trace cannot replay a call byte for byte → accepted: the barrier is
  deliberate (a leaked secret in the trace would outlive the proposal), and the
  documentation says that "raw" means raw after redaction.
- A CLI single call is a coarser unit than an API call → the overshoot is one whole CLI call,
  bounded by the container timeout; the guarantee per turn of an agent-mode CLI is a
  requirement before that runtime is released (spec 0009 AC-7).
- The soft ceiling lets a run overshoot by one call → accepted and documented; a hard cap
  would need the provider to refuse a call by price, which APIs do not offer. The
  per-call output limit keeps that overshoot small.
