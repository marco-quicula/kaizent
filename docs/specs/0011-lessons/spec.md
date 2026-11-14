# Spec 0011: Lessons

- **Status:** Approved
- **Wave:** 1
- **Depends on:** 0007, 0008, 0009, 0010
- **Related ADRs:** 0011, 0012, 0027

## Goal

Close the PDCA loop with the Act step. When a reviewer edits or rejects a proposal,
Kaizent drafts a **lesson**: a short knowledge entry capturing what was missing or wrong.
A human reviews the draft, and once approved it becomes part of the knowledge base, so
that the next similar ticket benefits from it.

## User stories

- As a **reviewer**, I want my corrections to turn into knowledge, so that I do not have
  to correct the same thing twice.
- As a **workspace owner**, I want every lesson to be approved by a human and stored as
  a plain Markdown file, so that the knowledge base stays trustworthy and reviewable in
  git.

## Acceptance criteria

- **AC-1:** On `VerdictRecorded` with type `APPROVED_WITH_EDITS` or `REJECTED`, the
  `learning` module makes one `LlmGateway` call (profile `lesson`). The call receives:
  - the problem;
  - the triage result;
  - the proposal (diagnosis, reply, citations);
  - the final reply or the rejection comment;
  - the edit ratio.

  It returns `LessonDraft { worthLearning, title, content, tags, rationale }`. The call
  has a budget and a deadline of its own (`kaizent.budget.lesson`, spec 0009 AC-12),
  separate from the run's, which may be spent by then. Before anything is stored, the
  draft (title, content, tags and rationale) passes the **output check** of spec 0008
  AC-12 (ADR-0032, layer 4). A lesson becomes knowledge that other runs read, so a draft
  that carries a leak or an injected instruction is an indirect injection vector; a
  redaction is flagged on the lesson, and the reviewer is warned.
- **AC-2:** The outcome of **every** processed verdict of those types is persisted, as
  `DRAFTED`, `NO_LESSON` or `FAILED`. With `worthLearning = false`, no lesson is created,
  the outcome is `NO_LESSON`, and an event "no lesson needed" is added to the run's trace
  with the rationale. Typical cases are cosmetic edits and tone-only changes.
- **AC-3:** With `worthLearning = true`, a `Lesson` is stored with status `PROPOSED`,
  linked to the run and its subject, with the checked text (AC-1), and the outcome is
  `DRAFTED`.
- **AC-4:** A verdict is **concluded** once its outcome is `DRAFTED` or `NO_LESSON`. A
  concluded verdict is never drafted again, even if the event is delivered more than
  once: at most one lesson per verdict. This does **not** promise exactly one model call:
  if the process stops after the call and before the outcome is saved, the call is made
  again on the retry (AC-5), and its cost counts against the lesson budget.
- **AC-5:** A drafting failure (a model error, an invalid output after the repair, an
  exceeded budget or deadline, or a crash) is **recoverable**, not a conclusion: it is
  recorded as an `ERROR` trace event on the run and as the outcome `FAILED`, with the
  attempt count, and the verdict is retried, up to `kaizent.learning.max-attempts`
  (default 3), until it is concluded. The verdict and the delivery are not affected.
- **AC-6:** **Lessons page:** a list of proposed lessons with title, subject, source
  ticket and age. A lesson can be opened to see the content, the rationale and a link to
  the source run. The title and content can be edited before approval.
- **AC-7:** **Approve** writes `knowledge/lessons/<yyyy-MM-dd>-<slug>.md` through
  `KnowledgeWriter`. The file has front matter (`lesson`, `subject`, `sourceRun`,
  `sourceTicket`, `approvedAt`, `tags`) followed by the content. The knowledge index is
  updated, and the lesson's status becomes `APPROVED`, with its path. Approving is
  **resumable and never duplicates**:
  - the file name uses the lesson's **stable identifier**
    (`<yyyy-MM-dd>-<slug>-<short id>.md`, where the date is the day of approval, fixed
    when the approval starts), so that a repetition always targets the same file and never
    creates another one; there is no numeric suffix;
  - approving first records the intent: the status becomes `PUBLISHING`, with the final
    title, content, path and `approvedAt`, in one transaction;
  - then the file is written (a second write of the same content is a no-op, spec 0007
    AC-9), the index is updated (repeatable) and the status becomes `APPROVED`;
  - a failure or a crash between any two of these steps leaves the lesson in `PUBLISHING`,
    and approving it again (or the resume at startup) continues from the intent without a
    second file or a second effect.
- **AC-8:** **Discard** sets the status to `DISCARDED`, with an optional comment.
- **AC-9:** Approved lessons can be found through `search_knowledge` by later runs of
  roles whose `knowledgePaths` include `lessons`.
- **AC-10:** Lesson changes are pushed through the SSE stream (`lesson.updated`).
- **AC-11:** **Full-cycle test.** One integration test with the fake adapters covers the
  whole PDCA cycle and is the **closing criterion of wave 1**: a ticket is taken from the
  inbox, triaged and resolved with tool calls, a reviewer edits and approves the proposal,
  the reply appears in `outbox/`, the drafted lesson is approved and published exactly once,
  and a second, similar ticket finds the lesson through `search_knowledge` and cites it.

## Evaluation cases

These cases are run manually: reject or edit a run, then approve the lesson.

- **EV-1:** Reject the PC-006 proposal with the comment "Administrators can add pet types
  through the database script `data.sql`; tell the customer to contact their
  administrator." → the draft is `worthLearning`, and its content mentions the
  administrator and the data change.
- **EV-2:** Approve the EV-1 lesson, then submit a new ticket similar to PC-006 → the
  proposal cites the new lesson file.
- **EV-3:** Edit and approve PC-002, changing only punctuation and greeting →
  `worthLearning = false`.

The full cycle with real models is checked by these cases; AC-11 checks it offline, in CI.

## Out of scope

- Lessons that modify existing knowledge files, or skill improvement proposals (wave 2).
- Automatic de-duplication of lessons (a later wave, with vector search). (Not to be
  confused with AC-7: approving the same lesson twice never writes it twice.)

## Open questions

None.
