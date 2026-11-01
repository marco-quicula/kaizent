# Plan 0012: Subject metrics and autonomy levels

## Components

| Module | Component | Responsibility |
|---|---|---|
| `metrics` | `SubjectMetricsQuery` (`adapter.jdbc`) | One SQL query with window functions over `run` (verdict JSONB, triage subject), with `WHERE review_mode = 'HUMAN'`, so evaluation runs are excluded whatever their status, including `FAILED` |
| `metrics` | `AutonomyPolicy` | A pure function from metrics and `AutonomyProperties` to a level and the progress toward the next one |
| `metrics` | `SubjectMetricsService` | Joins the declared subjects (from `WorkspaceConfig`) with the query results; includes `unknown` |
| `api` | `SubjectsController` | `GET /subjects`, `GET /subjects/{id}/runs` |
| console | `SubjectsPage`, `SubjectDetail` | Table, badges, progress hints, recent runs |

## API

- `GET /subjects` → `SubjectMetrics[] { id, name, totals, rates { overall, window },
  avgConfidence { overall, byVerdict }, avgTokens, avgApiCost, autonomy { level,
  provisional: true, nextLevel, missing[] } }`;
- `GET /subjects/{id}/runs?page=` → `RunSummary[]`.

## Configuration

```yaml
kaizent.autonomy:
  window: 20
  mid-level:  { min-window: 10, min-approved-as-is: 0.60 }
  senior:     { min-window: 20, min-approved-as-is: 0.80, max-rejected: 0.05 }
  specialist: { min-total: 50, min-approved-as-is: 0.90, max-rejected: 0.0 }
```

## Testing

| AC | Test |
|---|---|
| AC-1, AC-2, AC-7 | Query integration tests on seeded runs, including evaluation runs in `EVALUATED` and in `FAILED`, both excluded |
| AC-3, AC-4 | `AutonomyPolicy` table-driven unit tests at each boundary |
| AC-5 | Orchestrator test with seeded Specialist metrics |
| AC-6 | Console tests |

## Risks and mitigations

- A query over JSONB gets slow as runs grow → indexes on `(triage->>'subjectId')` and on
  the verdict type; a materialized view if needed later.
