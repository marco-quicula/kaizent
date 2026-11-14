# Spec 0008: Resolution and proposal

- **Status:** Approved
- **Wave:** 1
- **Depends on:** 0004, 0006, 0007
- **Related ADRs:** 0016, 0017, 0022, 0029, 0033

## Goal

Replace the skeleton's single prompt with the real Do step:

- assemble the agent task from the role, the problem, the skills catalog and the
  knowledge scope;
- run it through an `AgentRuntime`: in wave 1, the native Spring AI loop with Kaizent's
  tools (the CLI agent runtime is wave 2, ADR-0033);
- validate the proposal and its citations;
- compute the adjusted confidence.

## User stories

- As a **reviewer**, I want each proposal to cite its sources and the skills it used, so
  that I can verify it quickly.
- As the **maintainer**, I want the same resolution behavior whichever API model runs the
  loop, so that I can switch providers freely.

## Acceptance criteria

- **AC-1:** For a run in `RESOLVING`, an `AgentTask` is built from:
  - the role persona;
  - the problem (title, body, problem type);
  - the target reply language from triage;
  - the role's skills catalog (names, descriptions, versions);
  - the role's knowledge roots;
  - the `Proposal` output schema;
  - the run budget.

  The instructions come from `prompts/resolution.md`.
- **AC-2:** The **native runtime** exposes three tools, each restricted to the role's
  scope: `search_knowledge(query, limit ≤ 10)`, `read_document(path, section?)` and
  `read_skill(name)`. A call outside the scope returns a tool error to the model and does
  not throw. Each tool result is capped at `kaizent.agent.max-tool-result` (default 16 KB).
  A longer result is cut, carries an explicit truncation marker (how much was returned, out
  of how much) and, for a document, the outline of its section headings, so that the model
  can read another section with `read_document(path, section)`. The cap applies to what is
  sent to the model; the trace keeps the result summary (spec 0009 AC-3).
- **AC-3:** The native runtime stops when the model returns the final structured answer,
  or when the maximum number of tool calls (`kaizent.budget.max-tool-calls`, default 20)
  is reached. In the second case, the run fails with `BUDGET_EXCEEDED`. Before each model
  call, the context sent (instructions, messages and tool results so far) plus a reserve
  for the output (`kaizent.budget.max-output-per-call`) must fit the model's context limit
  (`kaizent.agent.max-context-tokens`); otherwise the run fails with `BUDGET_EXCEEDED`
  and an `ERROR` event that names the context limit. The check is the one of spec 0009
  AC-8, made before every model call.
- **AC-4:** *Moved to wave 2 (ADR-0033).* The CLI runtime, which would receive only the
  role's knowledge roots and skill folders as read-only paths, is not part of wave 1: the
  sandbox of Codex lets an agent read the whole container, including the tool's login.
  The wave 1 resolution runtimes are `native` and `fake`, and both answer with the
  `Proposal` schema of AC-5.
- **AC-5:** The `Proposal` contains:
  - `diagnosis` (English);
  - `suggestedReply` (in the triage language);
  - `rawConfidence` (0 to 1) and `confidenceRationale`;
  - `citations[]`, each with a `path` relative to the workspace and an optional
    `section`;
  - `skillsUsed[]`;
  - `lacksInformation` (boolean).
- **AC-6:** **Citation validation:** every citation path must exist and lie inside the
  role's knowledge roots. Invalid citations are kept but flagged.
- **AC-7:** **Skills used:** for the native runtime, they are the skills actually read
  through `read_skill`. Each one is recorded with its version.
- **AC-8:** **Adjusted confidence** follows ADR-0022:
  - no citation → capped at 0.5;
  - an invalid citation → capped at 0.3;
  - `lacksInformation` → capped at 0.3.

  The rules applied are recorded on the proposal.
- **AC-9:** The run moves to `AWAITING_REVIEW` with the proposal stored. An invalid
  output after the repair attempt moves the run to `FAILED` with reason `ERROR`.
- **AC-10:** `SkeletonResolver` is removed, and the end-to-end test of spec 0002 passes
  with the fake `AgentRuntime`.
- **AC-11:** **Suspicious tickets** (spec 0006 AC-9; ADR-0032, layers 2 and 3). In wave 1
  the only resolution runtimes are `native` and `fake` (ADR-0033), and the native one has
  least capability by construction: Kaizent's three read tools over the role's scope, no
  shell and no other file access. A suspicious ticket is therefore resolved like any other
  ticket, and its risk, reasons and notes are shown to the reviewer. The runtime that
  resolved the ticket (`resolvedBy`) is recorded on the proposal. `SAFE` only means that no
  rule matched.

  The routing policy of layer 3 (`kaizent.security.suspicious-tickets`, with `api-runtime`
  and `flag-only`, `kaizent.security.api-runtime`), the refusal of a CLI preset with
  `read-restricted: false` as a resolution runtime and the startup WARN are the contract
  of the wave 2 CLI runtime and are implemented there. In wave 1 the application refuses
  to start with `kaizent.agent.resolution.runtime: cli` (spec 0004 AC-8).
- **AC-12:** **Output check** (ADR-0032, layer 4). It is **one barrier**, applied to every
  text the model produced before that text is stored or shown, whatever the step: the
  proposal, the model's text in the trace (including the raw responses, the invalid ones,
  the repair attempts, errors and unknown events, spec 0009 AC-2 and AC-4), the triage
  rationale (spec 0006) and the lesson draft (spec 0011). Before a proposal is stored, token and
  key formats (for example `eyJ…` JWTs, `sk-…` keys), paths outside the granted ones and
  URLs to hosts outside `kaizent.security.known-hosts` are redacted in every text the
  model produced that is stored or shown: the diagnosis, the suggested reply, the
  confidence rationale, each citation's path and section, and the model's own text in
  the resolution's trace events (its messages and its tool-call arguments). The proposal
  stays structurally valid (a redacted citation path is an invalid citation, AC-6). The
  proposal records each redaction as a flag, with the field, and the reviewer is
  warned.

## Evaluation cases

Run with a native-runtime API profile (the CLI runtime is wave 2, ADR-0033):

- **EV-1:** Across all 12 tickets, at least 90% of the citations are valid.
- **EV-2:** PC-001, PC-004 and PC-010 cite the matching known issue.
- **EV-3:** PC-009 and PC-012 do not invent features (`mentionsNone` passes).
- **EV-4:** PC-012 → `lowConfidence` and `lacksInformation = true`.
- **EV-5:** PC-003 → the reply is in Portuguese.
- **EV-6:** PC-010 and PC-011 (stack traces) → the role is `technical-support-engineer`,
  and the skills used include `diagnose-stack-trace`.

## Out of scope

- Tools beyond the three listed, and MCP tools (wave 2).
- Several attempts or self-critique loops (later, if evaluations show a need).

## Open questions

None.
