# Spec 0002: Walking skeleton

- **Status:** In progress
- **Wave:** 1
- **Depends on:** 0001
- **Related ADRs:** 0014, 0016, 0023, 0027, 0028

## Goal

Make the full cycle work end to end in its thinnest possible form: a ticket file in
`inbox/` becomes a run, a single model call produces a proposal, a human approves it in
the console, and the reply appears in `outbox/`. Every later spec deepens one step of
this path. Building the skeleton first exposes integration problems early.

## User stories

- As the **maintainer**, I want to drop a ticket file and see a proposed reply in the
  console, so that I can prove the cycle works before investing in each step.
- As a **reviewer**, I want to approve a proposal and get a reply file, so that the
  output of the cycle is tangible.

## Acceptance criteria

- **AC-1:** Given the workspace `examples/petclinic` with a ticket `inbox/PC-001.md`,
  when the application runs, then within one polling interval a `Problem` and a `Run` in
  status `RECEIVED` exist for it.
- **AC-2:** The same ticket is never turned into a second run, including across
  restarts.
- **AC-3:** Given a `RECEIVED` run, the worker moves it through `RESOLVING` to
  `AWAITING_REVIEW`, using a fixed default role and a single `LlmGateway` call that
  returns a proposal (`diagnosis`, `suggestedReply`, `rawConfidence`).
- **AC-4:** If the model call fails, then the run goes to `FAILED` with reason `ERROR`
  and an error message. The worker continues with the next run.
- **AC-5:** `GET /api/v1/runs` lists runs with their status, and `GET /api/v1/runs/{id}`
  returns the problem and the proposal.
- **AC-6:** `POST /api/v1/runs/{id}/verdict` with `{type: APPROVED}` on an
  `AWAITING_REVIEW` run moves it to `APPROVED` and then `DELIVERED`, and writes
  `outbox/PC-001.reply.md` in the format of ADR-0028.
- **AC-7:** A verdict on a run that is not `AWAITING_REVIEW` returns HTTP 409.
- **AC-8:** The console lists runs (refreshed by polling for now) and shows a run's
  detail with an **Approve** button. Approving shows the new status.
- **AC-9:** With the `fake` LLM adapter, an automated end-to-end test covers AC-1 to
  AC-6 without network access.
- **AC-10:** With the `gemini-api` adapter and a `GEMINI_API_KEY` in `.env`, the same
  cycle works manually against a real model.

## Evaluation cases

- **EV-1:** `PC-001` with the real adapter → a non-empty reply in English that mentions
  the telephone format.

## Out of scope

- Ticket validation, intake errors and warnings (0005); triage and roles (0006); skills
  and knowledge (0007); the agent loop, citations and confidence rules (0008); the trace
  and budgets (0009); edit and reject verdicts and live updates (0010); lessons (0011);
  metrics (0012); CLI adapters (0004).

## Open questions

None.
