# Plan 0003: PetClinic demo workspace and evaluation runner

## Modules and components

| Location | Component | Responsibility |
|---|---|---|
| `examples/petclinic/` | Workspace files | Layout per ADR-0027, with the content listed in the spec |
| `workspace` | `WorkspaceLoader`, `WorkspaceValidator`, `EvaluationCase` | Parse `kaizent-workspace.yaml`, roles, skills front matter and eval cases; report problems with file paths. At startup, the problems are logged as warnings and the application keeps running |
| `evaluation` (new module) | `EvalRunner` (`@Profile("eval")`, `ApplicationRunner`), `PropertyChecker`s, `EvalReportWriter` | Load the cases through `WorkspaceLoader`, submit each ticket as a run with `reviewMode = SKIP`, wait for completion, check properties, write the report |
| `run` | `ReviewMode` on `Run` | `HUMAN` (default) or `SKIP` (evaluation only); skipped runs end in `EVALUATED` and are never delivered |

`EVALUATED` is a terminal status used only by evaluation runs. An evaluation run can also
end in `FAILED`, so **`review_mode = SKIP` is the persistent marker of an evaluation
run, whatever its status**. Every consumer that must exclude evaluation runs (the
console lists, the metrics) filters on `review_mode`, never on the status. `GET /runs`
lists only `HUMAN` runs; `GET /runs/{id}` still shows an evaluation run, and a verdict on
it is rejected because it is not awaiting review.

## Data

- `V3__run_review_mode.sql`: adds `review_mode` and the `EVALUATED` status value.

## File formats

```yaml
# kaizent-workspace.yaml
name: PetClinic demo
description: Synthetic support workspace for Spring PetClinic.
defaultRole: support-agent
subjects:
  - id: owners
    name: Owners
    description: Registering, finding and editing pet owners.
```

```markdown
<!-- roles/support-agent.md -->
---
id: support-agent
name: Support agent
subjects: [owners, pets, visits]
allowedSkills: [answer-how-to-question, diagnose-reported-error, write-customer-reply]
knowledgePaths: [product, faq.md, known-issues, lessons]
---
You are a friendly, precise first-level support agent for PetClinic...
```

```yaml
# evals/cases/PC-009.yaml
ticket: PC-009
expect:
  subject: visits
  problemType: HOW_TO
  citesAny: [knowledge/product/visits.md]
  mentionsAll:
    # Each item is a phrase, or a list of alternatives of which one must appear.
    - ["not supported", "not possible", "cannot be cancelled", "no way to cancel"]
  mentionsNone: []
  lowConfidence: false
  ticketLanguage: en
  replyLanguage: en
```

`ReplyLanguageChecker` uses [Lingua](https://github.com/pemistahl/lingua) (Apache-2.0),
restricted to the languages declared in the evaluation cases for accuracy. It runs on
`suggestedReply` and never on triage output.

## Configuration

The cases are read from the workspace's `evals/cases/`; `kaizent.workspace.path` chooses
the workspace.

- `kaizent.eval.output-path` (default `target/eval`).
- `kaizent.eval.case-timeout` (default 10 minutes).
- `kaizent.eval.poll-interval` (default 1 second): how often the runner checks whether a
  run has finished.
- `kaizent.eval.exit-when-done` (default `true`): stop the application after the report.

The `eval` profile (`application-eval.yaml`) starts no HTTP server and turns intake
polling off (`kaizent.intake.polling=false`), so the inbox is not queued for human review
while the cases run.

## How the runner submits a case

The runner stores the case's ticket as a new problem through `EvaluationProblems`, an
intake port that publishes no `ProblemReceived`, so no review run is created. Its source
is unique per session (`evaluation-<timestamp>`), so each session reads the current
ticket file. It then starts a `SKIP` run through `EvaluationRuns` and waits until the
worker has finished it. Cases run one after another, so each duration is the case's own.
If a case does not finish within `kaizent.eval.case-timeout`, the session stops and the
remaining cases are reported as `NOT RUN`: the single worker is still busy with the slow
run, so they would only time out behind it.

Properties that the pipeline does not produce yet are reported as skipped, naming the
spec that adds them: triage (subject, problem type, ticket language) in spec 0006, and
citations and the adjusted confidence in spec 0008. A run that produced no proposal fails
every property. Tokens and cost are reported as not recorded until usage tracking (specs
0004 and 0009).

## Testing

| AC | Test |
|---|---|
| AC-1 | `WorkspaceValidatorTest` on the real `examples/petclinic` and on broken fixtures |
| AC-2 | A test that checks for a `README.md` and the synthetic-data notice |
| AC-3, AC-6 | `PropertyChecker` unit tests |
| AC-4, AC-5 | `EvalRunner` test in `--dry-run` mode with fake adapters |

## Risks and mitigations

- The knowledge base disagrees with the real PetClinic behavior → the knowledge base is
  declared the demo's source of truth, and `product/overview.md` names the PetClinic
  commit it describes.
- The checkers are too strict or too loose for natural-language replies → keep keyword
  checks short and few, and review them when cases fail for wording reasons.
