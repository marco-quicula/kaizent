# Plan 0003: PetClinic demo workspace and evaluation runner

## Modules and components

| Location | Component | Responsibility |
|---|---|---|
| `examples/petclinic/` | Workspace files | Layout per ADR-0027, with the content listed in the spec |
| `workspace` | `WorkspaceLoader`, `WorkspaceValidator` | Parse `kaizent-workspace.yaml`, roles, skills front matter and eval cases; report problems with file paths |
| `evaluation` (new module) | `EvalRunner` (`@Profile("eval")`, `ApplicationRunner`), `EvalCase`, `PropertyChecker`s, `EvalReportWriter` | Load cases, submit each ticket as a run with `reviewMode = SKIP`, wait for completion, check properties, write the report |
| `run` | `ReviewMode` on `Run` | `HUMAN` (default) or `SKIP` (evaluation only); skipped runs end in `EVALUATED` and are never delivered |

`EVALUATED` is a terminal status used only by evaluation runs. An evaluation run can also
end in `FAILED`, so **`review_mode = SKIP` is the persistent marker of an evaluation
run, whatever its status**. Every consumer that must exclude evaluation runs (the
console lists, the metrics) filters on `review_mode`, never on the status.

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
  mentionsNone: ["click Cancel", "click Reschedule"]
  lowConfidence: false
  ticketLanguage: en
  replyLanguage: en
```

`ReplyLanguageChecker` uses [Lingua](https://github.com/pemistahl/lingua) (Apache-2.0),
restricted to the languages declared in the evaluation cases for accuracy. It runs on
`suggestedReply` and never on triage output.

## Configuration

- `kaizent.eval.cases-path` (default `<workspace>/evals/cases`).
- `kaizent.eval.output-path` (default `target/eval`).
- `kaizent.eval.case-timeout` (default 10 minutes).

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
