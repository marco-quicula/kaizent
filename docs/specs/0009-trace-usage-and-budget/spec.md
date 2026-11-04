# Spec 0009: Run trace, usage and budget

- **Status:** Approved
- **Wave:** 1
- **Depends on:** 0004, 0008
- **Related ADRs:** 0016, 0020, 0023

## Goal

Make every run fully transparent and cost-bounded: a durable, ordered trace of every
step, model call and tool call; token and cost totals; enforceable per-run budgets; and
retries of failed runs. The trace is also the data capture that later evaluation and
fine-tuning depend on.

## User stories

- As a **reviewer**, I want to see step by step what the agent did, so that I can trust or
  correct it.
- As the **maintainer**, I want each run's tokens and cost, and hard limits, so that
  spending is visible and bounded.
- As a **future evaluator**, I want the full input and output of every model call stored,
  so that runs can be replayed and turned into datasets.

## Acceptance criteria

- **AC-1:** Each run has an ordered list of `TraceEvent`s with types `STATE_CHANGED`,
  `STEP_STARTED`, `STEP_COMPLETED`, `LLM_CALL`, `TOOL_CALL`, `AGENT_EVENT` and `ERROR`,
  each with a step, a summary, details (JSON), a timestamp and, when relevant, a usage
  and a duration.
- **AC-2:** `LLM_CALL` events store the adapter, the model or preset, the prompt version,
  the full request messages and the raw response, each capped at
  `kaizent.trace.max-payload` (default 256 KB, with a truncation marker).
- **AC-3:** `TOOL_CALL` events store the tool name, the arguments, a result summary
  (paths and headings, not full documents) and the duration.
- **AC-4:** For CLI runtimes, the mapped tool events become `AGENT_EVENT`s. Unmapped raw
  events are kept in the `LLM_CALL` details.
- **AC-5:** The assembled `AgentTask` (instructions, skills catalog, knowledge roots,
  budget) is stored as the details of the resolution `STEP_STARTED` event.
- **AC-6:** The run's usage totals (input and output tokens, cost, and the cost type
  summary) are updated after each `LLM_CALL`. A run that mixes cost types reports each
  one separately.
- **AC-7:** Per-run budgets: `max-input-tokens`, `max-output-tokens`, `max-cost`,
  `max-tool-calls` and `timeout`. Exceeding a token, cost or tool limit fails the run
  with `BUDGET_EXCEEDED`. Exceeding the wall-clock timeout fails it with `TIMEOUT`. Both
  leave an `ERROR` event explaining which limit was hit.
- **AC-8:** Before an API call, Kaizent estimates the input tokens. If the estimate
  already exceeds the remaining budget, it fails without calling.
- **AC-9:** `POST /api/v1/runs/{id}/retry` on a `FAILED` run increments `attempt`, keeps
  the previous events (tagged with their attempt) and moves the run to `RECEIVED`. A
  delivery failure is retried at delivery (ADR-0023). A retry on any other status
  returns 409.
- **AC-10:** `GET /api/v1/runs/{id}/trace` returns the events in order, filterable by
  attempt. `GET /runs/{id}` includes the usage totals and the budget.
- **AC-11:** Trace writes happen in the same transaction as the state change they
  belong to. A crash never leaves a state change without its event.

## Evaluation cases

None. The evaluation report (spec 0003) uses the usage totals.

## Out of scope

- Trace retention or purge policies, and export to OpenTelemetry (wave 5).
- Dataset export for fine-tuning (wave 6).
- Reconciling the estimated cost with the providers' usage or billing APIs (an idea for
  wave 5, in the roadmap). This spec records the estimate only, from the tokens of each
  response and the configured prices, with no extra provider calls.

## Open questions

None.
