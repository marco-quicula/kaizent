# Plan 0010: Review console

## Backend components

| Module | Component | Responsibility |
|---|---|---|
| `review` | `ReviewService` | Full verdict rules (AC-6, AC-7); publishes `VerdictRecorded` |
| `review` | `EditRatio` | Normalized Levenshtein distance between proposal and final reply (0 = identical, 1 = completely different) |
| `api` | `EventStreamController` | `SseEmitter` registry; listens to `RunStatusChanged` and `LessonChanged` application events |
| `api` | `SystemController` | Extended for AC-10 |

## Console structure

```
src/
├── api/            # generated types (openapi-typescript) and openapi-fetch client
├── events/         # useEventStream hook: EventSource, reconnects, invalidates TanStack Query caches
├── pages/
│   ├── runs/       # RunsPage, RunDetailPage, VerdictPanel, TraceTimeline, CitationDrawer
│   ├── intake/     # IntakePage
│   └── system/     # SystemPage
└── components/     # ConfidenceBadge, StatusBadge, MarkdownView, UsageSummary
```

- `MarkdownView` uses `react-markdown` (MIT) with GitHub Flavored Markdown, with raw
  HTML disabled.
- `VerdictPanel` uses Mantine forms. Reject requires a comment, and the editor is a
  textarea with a preview tab.

## Data

`verdict` JSONB gains `editRatio` and `reviewer`. No migration is needed, because the
JSON shape is versioned in code.

## API

- `POST /runs/{id}/verdict`: `VerdictRequest { type: APPROVED | APPROVED_WITH_EDITS |
  REJECTED, finalReply?, comment? }`. Returns 400 when the type's rules are violated and
  409 when the run is in the wrong state.
- `GET /events`: `text/event-stream`, with the events `run.updated {id, status}` and
  `lesson.updated {id, status}`.
- `GET /runs`: adds `includeEvaluations` (default false).

## Testing

| AC | Test |
|---|---|
| AC-1, AC-3 to AC-5, AC-9 to AC-11 | Vitest and React Testing Library page tests with mocked API responses |
| AC-2 | SSE controller integration test, plus a hook test with a mock `EventSource` |
| AC-6, AC-7 | `ReviewService` unit tests and API tests |
| AC-12 | Testing Library queries by role and label; manual keyboard check |

## Risks and mitigations

- SSE connections behind proxies → local only in wave 1; the hook falls back to polling
  every 10 seconds when the stream fails repeatedly.
