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
- **AC-2:** `LLM_CALL` events store the adapter, the model or preset requested, the model
  or version that the provider reported in its answer (they can differ, for example when an
  alias moves), the prompt version, the full request messages and the raw response, each capped at
  `kaizent.trace.max-payload` (default 256 KB, with a truncation marker). Everything the
  model produced passes the **output check** of spec 0008 AC-12 (ADR-0032, layer 4)
  before it is persisted, so "raw" means *raw after redaction*. The rule is the same for
  every path that stores model text: a valid response, an invalid response, the repair
  attempt (its request, which quotes the previous answer, and its response), an error
  message that echoes model text, and an unknown event. Each redaction is flagged on the
  event. The stored trace therefore is not a byte-exact copy of what the model returned,
  and the documentation says so.
- **AC-3:** `TOOL_CALL` events store the tool name, the arguments (after the same output
  check), a result summary (paths and headings, not full documents), the duration and, for
  the read tools, the **provenance of what was returned**: `contentHash`, the SHA-256 of the
  text actually returned (the section or the document, after the cap), and `documentHash`,
  the SHA-256 of the whole document on disk, which later detects a change whatever the cap
  or the section was; and, when the workspace is a git working copy, the commit version
  (spec 0007 AC-3 and AC-6). A commit alone does not identify the content read, because of
  uncommitted changes and new lessons.
- **AC-4:** For CLI runtimes (wave 2, ADR-0033), the mapped tool events become
  `AGENT_EVENT`s. Unmapped raw events are kept in the `LLM_CALL` details, after the same
  redaction (AC-2). In wave 1 the type `AGENT_EVENT` exists, and the native runtime may use
  it for its own loop steps.
- **AC-5:** The assembled `AgentTask` (instructions, skills catalog, knowledge roots,
  budget) is stored as the details of the resolution `STEP_STARTED` event, together with
  the run's **effective configuration**, which is also stored on the run: for each step the
  adapter or preset, the model requested, the parameters in force (for example the
  temperature, when set), the content hash of each prompt, the version of the injection
  detector (spec 0006 AC-9) and the versions of the policies applied (confidence caps,
  output check, budgets). The model that the provider reported is recorded per call (AC-2).
  The configuration is a snapshot: later changes to the settings do not alter it, so that
  results can be compared by configuration (spec 0012).
- **AC-6:** The run's usage totals (input and output tokens, cost, and the cost type
  summary) are updated after each **effective provider call**: every model turn of the
  agent loop, every repair attempt and every retry (spec 0004 AC-2 and AC-4) counts, not
  only each `LlmGateway.complete` or `AgentRuntime.run`. A run that mixes cost types
  reports each one separately. A call whose usage the provider did not report is recorded
  as **unknown**, never as zero: the totals show how many calls were unknown, and the
  cost type is `UNKNOWN` for them (spec 0004 AC-5). The call is also marked **estimated**
  (AC-8), because the guard debited something for it.
- **AC-7:** Per-run budgets: `max-input-tokens`, `max-output-tokens`, `max-cost`,
  `max-tool-calls` and `timeout`. Exceeding a token, cost or tool limit fails the run
  with `BUDGET_EXCEEDED`. Exceeding the wall-clock timeout fails it with `TIMEOUT`. Both
  leave an `ERROR` event explaining which limit was hit. The budget is a **soft
  ceiling**: it is checked before each call (AC-8) and the usage is known only after it,
  so the most a run can exceed it is **one call**, and the documentation says so. The
  remaining time is also checked before each call, and a retry never waits past it. The
  guarantee has **two levels**, because the unit of a call differs:
  - **Native runtime and API adapters:** the unit is the provider call, including each
    repair attempt, retry and loop turn (AC-8). The most a run can exceed a limit is one
    such call.
  - **CLI adapters, single calls (wave 1: triage, lesson, the model check):** the unit is
    the `CliExecutor` call. The balance is checked before each one. The tool's own turns and
    retries inside it are not visible to Kaizent, so the most a run can exceed a limit is
    **one whole CLI call**, bounded by the container timeout (ADR-0030). Its usage arrives
    aggregated, after the call, and may be unknown (AC-6), never zero.

  The **CLI in agent mode is outside wave 1** (ADR-0033). A **per-turn guarantee for it is
  a requirement before that runtime is released** (a turn limit of the tool when there is
  one, the timeout, or calls mediated by Kaizent, such as through MCP). The single-call
  guarantee above does not cover it.
- **AC-8:** The remaining budget is checked **before every provider call**, including
  each repair attempt, each retry and each turn of the agent loop (for a CLI single call,
  before each `CliExecutor` call, as AC-7 says). Kaizent estimates the
  input tokens and, if the estimate already exceeds the remaining input budget, or the
  remaining cost or time is exhausted, it fails without calling. When the provider
  accepts an output limit, Kaizent sends one: the smaller of the remaining output budget
  and the per-call cap. Usage that is unknown (AC-6) is **debited at the conservative
  upper bound of the call**, so that the next call is still barred in time:
  - input tokens: the estimate;
  - output tokens: the output limit sent to the provider;
  - cost: those two values at the configured price; without a configured price, the cost
    dimension does not limit, and the token limits apply.

  The call is marked **estimated**, and the reported total stays unknown, never zero.
  The guarantee degrades explicitly: when the provider accepts no output limit and
  reports no usage, nothing bounds one call from above, and the guarantee becomes "the
  next call is barred by the estimated balance". It is a guarantee on estimates, not on
  real usage.
- **AC-9:** `POST /api/v1/runs/{id}/retry` on a `FAILED` run increments `attempt`, keeps
  the previous events (tagged with their attempt) and moves the run to `RECEIVED`. A
  delivery failure is retried at delivery (ADR-0023). A retry on any other status
  returns 409.
- **AC-10:** `GET /api/v1/runs/{id}/trace` returns the events in order, filterable by
  attempt. `GET /runs/{id}` includes the usage totals and the budget.
- **AC-11:** Trace writes happen in the same transaction as the state change they
  belong to. A crash never leaves a state change without its event.
- **AC-12:** The lesson drafting of spec 0011 has a **budget and a deadline of its own**
  (`kaizent.budget.lesson`), separate from the run's: it starts after the verdict, when
  the run's budget may already be spent, and exceeding it fails the drafting, not the
  run. Its calls are checked, limited and recorded as in AC-6 to AC-8.

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
