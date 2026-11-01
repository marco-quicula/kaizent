# Plan 0011: Lessons

## Components

| Module | Component | Responsibility |
|---|---|---|
| `learning` | `LessonDraftingListener` | `@ApplicationModuleListener` on `VerdictRecorded`; filters the verdict types; idempotent on `run_id` |
| `learning` | `LessonDrafter` | Builds the prompt from `prompts/lesson.md` and calls `LlmGateway` (step `lesson`) with `LessonDraftOutput`; records trace events through the run's `TraceSink` |
| `learning` | `Lesson`, `LessonRepository` | Aggregate and persistence |
| `learning` | `LessonService` | Approve (slug, collision suffix, front matter, `KnowledgeWriter`) and discard; publishes `LessonChanged` |
| `api` | `LessonsController` | Endpoints |
| console | `LessonsPage`, `LessonDetail` | List, edit, approve and discard |

## Data

`V7__lesson.sql`: `lesson(id uuid, run_id uuid unique, subject_id text, title text,
content text, tags text[], rationale text, status text, path text, comment text,
created_at, decided_at)`.

## API

- `GET /lessons?status=` → `Lesson[]`;
- `GET /lessons/{id}`;
- `POST /lessons/{id}/approve { title?, content? }` → `Lesson`;
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
| AC-1 to AC-5 | Listener and drafter tests with the fake `LlmGateway` (worth learning, not worth, failure, duplicate event) |
| AC-6, AC-8, AC-10 | API tests and console tests |
| AC-7 | `LessonService` tests (file content, slug, collision) |
| AC-9 | Integration test: approve, then search finds the file |
| EV-1 to EV-3 | Manual run, recorded in the PR |

## Risks and mitigations

- Low-quality lessons pollute the knowledge base → human approval is required, the title
  and content are editable, and discarding is one click.
- Lessons contradict the curated documentation → out of scope for automatic detection in
  wave 1; reviewers decide.
