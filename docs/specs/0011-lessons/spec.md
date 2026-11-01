# Spec 0011: Lessons

- **Status:** Approved
- **Wave:** 1
- **Depends on:** 0007, 0010
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

  It returns `LessonDraft { worthLearning, title, content, tags, rationale }`.
- **AC-2:** With `worthLearning = false`, no lesson is created, and an event "no lesson
  needed" is added to the run's trace with the rationale. Typical cases are cosmetic
  edits and tone-only changes.
- **AC-3:** With `worthLearning = true`, a `Lesson` is stored with status `PROPOSED`,
  linked to the run and its subject.
- **AC-4:** At most one lesson is drafted per verdict, even if the event is delivered
  more than once.
- **AC-5:** A drafting failure is recorded as an `ERROR` trace event on the run. The
  verdict and the delivery are not affected.
- **AC-6:** **Lessons page:** a list of proposed lessons with title, subject, source
  ticket and age. A lesson can be opened to see the content, the rationale and a link to
  the source run. The title and content can be edited before approval.
- **AC-7:** **Approve** writes `knowledge/lessons/<yyyy-MM-dd>-<slug>.md` through
  `KnowledgeWriter`. The file has front matter (`lesson`, `subject`, `sourceRun`,
  `sourceTicket`, `approvedAt`, `tags`) followed by the content. The knowledge index is
  updated, and the lesson's status becomes `APPROVED`, with its path. A name collision
  appends a numeric suffix.
- **AC-8:** **Discard** sets the status to `DISCARDED`, with an optional comment.
- **AC-9:** Approved lessons can be found through `search_knowledge` by later runs of
  roles whose `knowledgePaths` include `lessons`.
- **AC-10:** Lesson changes are pushed through the SSE stream (`lesson.updated`).

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

## Out of scope

- Lessons that modify existing knowledge files, or skill improvement proposals (wave 2).
- Automatic de-duplication of lessons (a later wave, with vector search).

## Open questions

None.
