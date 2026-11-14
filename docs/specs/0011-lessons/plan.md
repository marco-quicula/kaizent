# Plan 0011: Lessons

## Components

| Module | Component | Responsibility |
|---|---|---|
| `learning` | `LessonDraftingListener` | `@ApplicationModuleListener` on `VerdictRecorded`; filters the verdict types; skips a verdict whose outcome is concluded (`DRAFTED` or `NO_LESSON`); records the outcome of every attempt. A `LessonRetrySweep` retries `FAILED` outcomes up to `kaizent.learning.max-attempts` |
| `learning` | `LessonDrafter` | Builds the prompt from `prompts/lesson.md` and calls `LlmGateway` (step `lesson`) with `LessonDraftOutput`, with its own `CallBudget` (`kaizent.budget.lesson`, spec 0009); passes the draft through the shared `OutputLeakCheck` before storing it; records trace events through the run's `TraceSink` |
| `learning` | `Lesson`, `LessonRepository` | Aggregate and persistence |
| `learning` | `LessonService` | Approve as a resumable publication (below) and discard; publishes `LessonChanged`. A `PublishingResume` runs at startup for lessons left in `PUBLISHING` |
| `api` | `LessonsController` | Endpoints |
| console | `LessonsPage`, `LessonDetail` | List, edit, approve and discard |

## Data

`V7__lesson.sql`:

- `lesson(id uuid, run_id uuid unique, subject_id text, title text, content text, tags
  text[], rationale text, flags jsonb, status text, path text, approved_at timestamptz,
  comment text, created_at, decided_at)`, with the status `PROPOSED`, `PUBLISHING`,
  `APPROVED` or `DISCARDED`;
- `lesson_drafting(run_id uuid primary key, outcome text, attempts int, error text,
  updated_at)`, with the outcome `DRAFTED`, `NO_LESSON` or `FAILED`. `DRAFTED` and
  `NO_LESSON` conclude the verdict; `FAILED` is recoverable.

**Publishing.** `approve` runs as: (1) in one transaction, set `PUBLISHING` with the final
title, content, `path` and `approved_at` (the file name is
`<yyyy-MM-dd>-<slug>-<first 8 characters of the lesson id>.md`, from the stored values, so
it is the same on every attempt); (2) `KnowledgeWriter.writeLesson(path, content)`, which is
a no-op for identical content; (3) the repeatable re-index; (4) set `APPROVED`. Every
step can be repeated, so `approve` on a `PUBLISHING` lesson, or the resume at startup,
continues from the stored intent. `approve` on an `APPROVED` lesson returns it unchanged.

## API

- `GET /lessons?status=` → `Lesson[]`;
- `GET /lessons/{id}`;
- `POST /lessons/{id}/approve { title?, content? }` → `Lesson` (idempotent; `title` and
  `content` count only on the first call, while the lesson is `PROPOSED`);
- `POST /lessons/{id}/discard { comment? }` → `Lesson`.

## Lesson file format

```markdown
---
lesson: 3f2a…
subject: pets
sourceRun: 9c1e…
sourceTicket: PC-006
approvedAt: 2026-11-15T14:03:00Z
tags: [pet-types, administration]
---
# Adding new pet types

Pet types are reference data...
```

## Testing

| AC | Test |
|---|---|
| AC-1 to AC-5 | Listener and drafter tests with the fake `LlmGateway`: worth learning, not worth (outcome `NO_LESSON` persisted, no second call on a duplicate event), failure (outcome `FAILED`, then a retry concludes it), a stop after the call and before the outcome (the call is made again, within the lesson budget), a lesson budget that is exhausted while the run's budget is also spent, and a draft that leaks a synthetic token (redacted and flagged) |
| AC-6, AC-8, AC-10 | API tests and console tests |
| AC-7 | `LessonService` tests: file content and name; approving twice; a failure injected after each step (after `PUBLISHING`, after the write, after the re-index) followed by a repeated approve and by the resume at startup: always one file, one index entry and the status `APPROVED`; two lessons with the same title get different files |
| AC-9 | Integration test: approve, then search finds the file |
| AC-11 | `FullCycleIT` (Testcontainers, fake adapters): inbox ticket → triage → resolution with tool calls (a scripted `ChatModel` behind the native runtime, or a fake `AgentRuntime` script that calls the `ToolScope` tools) → edit and approve → reply in `outbox/` → lesson drafted and approved, published once → a second similar ticket whose `search_knowledge` returns the lesson and whose proposal cites it. It extends the end-to-end test of spec 0002 and is the closing criterion of wave 1 |
| EV-1 to EV-3 | Manual run, recorded in the PR |

## Risks and mitigations

- Low-quality lessons pollute the knowledge base → human approval is required, the title
  and content are editable, and discarding is one click.
- A lesson drafted from a hostile ticket carries an injection or a leak into the knowledge
  base → the shared output check runs on the draft before it is stored, the reviewer
  sees the flags, and approval stays human (ADR-0032).
- A crash between the file write and the status change → the `PUBLISHING` intent and the
  stable file name make the retry safe (AC-7).
- Lessons contradict the curated documentation → out of scope for automatic detection in
  wave 1; reviewers decide.
