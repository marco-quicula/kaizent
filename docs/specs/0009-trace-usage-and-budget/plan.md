# Plan 0009: Run trace, usage and budget

## Components

| Module | Component | Responsibility |
|---|---|---|
| `run` | `TraceEvent`, `TraceEventRepository` | Persistence of events (append-only) |
| `run` | `RunTraceSink` (implements `TraceSink`) | Assigns the sequence numbers and the attempt; writes in the current transaction |
| `run` | `BudgetGuard` | Tracks the remaining budget per run; checked by the `LlmGateway` and `AgentRuntime` decorators |
| `llm` | `TracingLlmGateway` (decorator) | Wraps every `LlmGateway`: estimates tokens, checks the budget, records `LLM_CALL` with usage and duration |
| `resolution` | `TracingAgentRuntime` (decorator) | Same for `AgentRuntime`; native tool calls report through the `TraceSink`; enforces the wall-clock timeout |
| `run` | `RunRetryService` | Retry rules (AC-9) |

The decorators are applied by configuration, so adapters stay unaware of tracing and
budgets.

**Token estimation:** a character-based estimate (characters / 4), with a configurable
safety factor. It only guards against obvious overruns; the actual usage comes from the
provider.

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
  trace.max-payload: 256KB
```

## Testing

| AC | Test |
|---|---|
| AC-1 to AC-5 | Orchestrator tests with fakes, asserting the event sequences |
| AC-6 to AC-8 | `BudgetGuard` and decorator unit tests (the fake reports usage that exceeds the limits) |
| AC-9 | Retry service and API tests |
| AC-10 | API tests |
| AC-11 | A transaction test that forces a failure after the state change |

## Risks and mitigations

- Trace size grows quickly → payload caps; retention comes in a later wave.
- Storing full prompts keeps ticket content in the database → acceptable with synthetic
  data; redaction is an open decision before the first real connector.
