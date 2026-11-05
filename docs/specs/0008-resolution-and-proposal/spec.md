# Spec 0008: Resolution and proposal

- **Status:** Approved
- **Wave:** 1
- **Depends on:** 0004, 0006, 0007
- **Related ADRs:** 0016, 0017, 0022, 0029

## Goal

Replace the skeleton's single prompt with the real Do step:

- assemble the agent task from the role, the problem, the skills catalog and the
  knowledge scope;
- run it through an `AgentRuntime`, either the native Spring AI loop with Kaizent's tools
  or a CLI tool;
- validate the proposal and its citations;
- compute the adjusted confidence.

## User stories

- As a **reviewer**, I want each proposal to cite its sources and the skills it used, so
  that I can verify it quickly.
- As the **maintainer**, I want the same resolution behavior whether the loop runs on an
  API model or on a CLI tool, so that I can switch adapters freely.

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
  not throw.
- **AC-3:** The native runtime stops when the model returns the final structured answer,
  or when the maximum number of tool calls (`kaizent.budget.max-tool-calls`, default 20)
  is reached. In the second case, the run fails with `BUDGET_EXCEEDED`.
- **AC-4:** The **CLI runtime** (spec 0004) receives only the role's knowledge roots and
  allowed skill folders as read-only paths, and must answer with the same schema.
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
  through `read_skill`. For the CLI runtime, they are the self-reported names, filtered
  to the allowed skills. Each one is recorded with its version.
- **AC-8:** **Adjusted confidence** follows ADR-0022:
  - no citation → capped at 0.5;
  - an invalid citation → capped at 0.3;
  - `lacksInformation` → capped at 0.3.

  The rules applied are recorded on the proposal.
- **AC-9:** The run moves to `AWAITING_REVIEW` with the proposal stored. An invalid
  output after the repair attempt moves the run to `FAILED` with reason `ERROR`.
- **AC-10:** `SkeletonResolver` is removed, and the end-to-end test of spec 0002 passes
  with the fake `AgentRuntime`.
- **AC-11:** **Suspicious tickets** (spec 0006 AC-9) follow
  `kaizent.security.suspicious-tickets` (ADR-0032, layer 3):
  - `api-runtime` (default): resolved by the native runtime through
    `kaizent.security.api-runtime` (an API adapter and model), never by a CLI runtime.
    Without that setting, the run fails with reason `ERROR` and the message "suspicious
    ticket and no API runtime configured";
  - `flag-only`: the run continues with the configured runtime, and the reviewer sees
    the risk.

  Whatever the risk and the policy, a CLI preset with `read-restricted: false` is never
  a resolution runtime (ADR-0032, layer 1): every ticket is untrusted input, and `SAFE`
  only means that no rule matched. The application refuses to start when the resolution
  runtime, or `kaizent.security.api-runtime`, is such a preset, with a message that names
  it.

  `flag-only` logs a WARN at startup that names the setting and the risk, and the README
  documents it as the operator's risk. The runtime that resolved the ticket is recorded
  on the proposal.
- **AC-12:** **Output check** (ADR-0032, layer 4): before a proposal is stored, token and
  key formats (for example `eyJ…` JWTs, `sk-…` keys), paths outside the granted ones and
  URLs to hosts outside `kaizent.security.known-hosts` are redacted in every text the
  model produced that is stored or shown: the diagnosis, the suggested reply, the
  confidence rationale, each citation's path and section, and the model's own text in
  the resolution's trace events (its messages and its tool-call arguments). The proposal
  stays structurally valid (a redacted citation path is an invalid citation, AC-7). The
  proposal records each redaction as a flag, with the field, and the reviewer is
  warned.

## Evaluation cases

Run with both a native-runtime API profile and the Codex CLI preset:

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
