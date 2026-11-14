# Spec 0010: Review console

- **Status:** Approved
- **Wave:** 1
- **Depends on:** 0005, 0008, 0009
- **Related ADRs:** 0008, 0012, 0024, 0025

## Goal

Turn the skeleton's two screens into the console that a reviewer actually works in:

- a live run queue;
- a run detail page with the full reasoning trail;
- the three verdicts (approve, edit and approve, reject);
- the supporting pages: intake issues and system.

The Lessons page and the Subjects page are added by specs 0011 and 0012 on this
foundation.

## User stories

- As a **reviewer**, I want a live queue of runs awaiting review, so that I can work
  through it without refreshing.
- As a **reviewer**, I want to see the problem, the triage, the role, the proposal, the
  confidence, the citations, the skills and the trace in one place, so that I can decide
  quickly and with confidence.
- As a **reviewer**, I want to approve, edit and approve, or reject with a comment, so
  that my judgment is captured precisely.

## Acceptance criteria

- **AC-1:** **Runs page:** a table with ticket, title, subject, status, adjusted
  confidence band, cost and age. It can be filtered by status and subject, and "Awaiting
  review" is the default filter. Evaluation runs (`review_mode = SKIP`, whatever their
  status) are hidden unless the user asks for them.
- **AC-2:** **Live updates:** `GET /api/v1/events` (SSE) emits `run.updated` and
  `lesson.updated` with ids and statuses. The console updates lists and the open detail
  without a page reload, and reconnects automatically.
- **AC-3:** **Run detail page:**
  - the problem (rendered Markdown, with stack traces as code);
  - triage (subject, type, language, rationale) and role;
  - the proposal: diagnosis, suggested reply, confidence band with the raw value and
    the rules applied, citations with invalid ones flagged, skills used with versions;
  - usage and budget;
  - the run's effective configuration (adapter or preset, model requested and reported,
    parameters, prompt hashes, detector and policy versions; spec 0009 AC-5);
  - the failure reason when the run failed.
- **AC-4:** Clicking a citation opens the cited document (scrolled to the section) in a
  side panel, using `GET /knowledge/document`. The panel shows the **current** document,
  and says so. When its `contentHash` differs from the hash recorded for the citation at the
  time of the run (spec 0009 AC-3), the panel warns that the document has changed since the
  run. Showing the historical version is out of scope in wave 1.
- **AC-5:** **Trace timeline:** the events in order, grouped by step and attempt, with
  expandable details (prompts, responses, tool arguments).
- **AC-6:** **Verdicts** (`POST /runs/{id}/verdict`):
  - **Approve:** sends the suggested reply as is.
  - **Edit and approve:** an editor prefilled with the suggested reply. If the edited
    text equals the original after whitespace normalization, it is stored as
    `APPROVED`.
  - **Reject:** a comment is required.

  After a verdict, the page shows the new status and the delivery result.
- **AC-7:** The server stores, with each verdict, the final reply, the comment, the
  reviewer (`kaizent.review.reviewer-name`, default `maintainer`) and an **edit ratio**
  (normalized edit distance between the proposal and the final reply).
- **AC-8:** **Retry** is offered on failed runs (spec 0009).
- **AC-9:** **Intake page:** the intake issues (spec 0005), newest first, with severity.
- **AC-10:** **System page:** the workspace name and path, the adapter, model or preset,
  and cost type for each step, the budgets and the application version.
- **AC-11:** **Empty states** explain the next step (for example, "Drop a ticket file
  into `<workspace>/inbox/`").
- **AC-12:** Basic accessibility: keyboard navigation for the verdict actions, labels on
  form fields, and no information conveyed by color alone (bands also show text).

## Evaluation cases

None.

## Out of scope

- Authentication and multiple reviewers (wave 5).
- Showing the version of a cited document that the run actually read (only the warning of
  AC-4 is in wave 1).
- Bulk actions, search over ticket text, and dashboards (wave 4).

## Open questions

None.
