# Spec 0012: Subject metrics and autonomy levels

- **Status:** Draft
- **Wave:** 1
- **Depends on:** 0006, 0010
- **Related ADRs:** 0012, 0022

## Goal

Measure from day one. For each subject, compute how often proposals are approved as is,
approved with edits or rejected, together with confidence and cost, and derive a
**provisional autonomy level** (Junior, Mid-level, Senior, Specialist). In wave 1 the
level is displayed only: every proposal still requires human review.

## User stories

- As the **maintainer**, I want per-subject success rates, so that I can see where the
  agent is reliable and where it is not.
- As the **maintainer**, I want an autonomy level per subject, computed from real
  verdicts, so that I know which subjects could earn more autonomy in wave 4.

## Acceptance criteria

- **AC-1:** For each declared subject, plus `unknown`, `GET /api/v1/subjects` returns:
  - the totals: runs, reviewed, approved as is, approved with edits, rejected, failed;
  - the rates over all reviewed runs and over the last `window` reviewed runs (default
    20);
  - the average adjusted confidence, overall and per verdict type;
  - the average tokens and the average API cost per run.
- **AC-2:** Evaluation runs (`review_mode = SKIP`) are excluded from every metric,
  whatever their status. This includes failed evaluation runs.
- **AC-3:** The **provisional autonomy level** is computed from the last `window`
  reviewed runs. All thresholds are configurable:

| Level | Rule (default) |
|---|---|
| Junior | the default, or fewer than 10 reviewed runs |
| Mid-level | ≥ 10 reviewed in the window and an approved-as-is rate ≥ 60% |
| Senior | ≥ 20 reviewed in the window, approved as is ≥ 80% and rejected ≤ 5% |
| Specialist | ≥ 50 reviewed in total, approved as is in the window ≥ 90% and no rejection in the window |

- **AC-4:** The API and the console label the level as **"provisional, no effect in wave
  1"**, and also show the progress toward the next level (what is missing).
- **AC-5:** The level never changes behavior. An automated test asserts that a subject
  computed as Specialist still has every proposal waiting in `AWAITING_REVIEW`.
- **AC-6:** **Subjects page:** a table with these metrics and the level badge. A subject
  can be opened to see a list of its recent runs, which links to the run detail.
- **AC-7:** The metrics are computed on read, with SQL over `run`. They are correct
  immediately after a verdict, with no background job.

## Evaluation cases

None.

## Out of scope

- Acting on the level: automatic replies, sampling audits (wave 4).
- Confidence calibration charts, and outcome signals such as reopen rates or ratings
  (wave 4).
- Manual level overrides (wave 4).

## Open questions

None. The final thresholds are an open decision for wave 4, informed by the data
collected here.
