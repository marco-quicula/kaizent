# Plan 0006: Triage and roles

## Modules and components

| Module | Component | Responsibility |
|---|---|---|
| `triage` | `RoleCatalog` (port), `FileRoleCatalog` | Loads `roles/*.md` (front matter and persona) |
| `triage` | `RoleSelector` | Deterministic subject → role mapping |
| `triage` | `TriageService` | Builds the prompt from `prompts/triage.md`, calls `LlmGateway` (step `triage`) with `TriageOutput`, normalizes unknown subjects |
| `workspace` | `WorkspaceValidator` | Extended with role rules (AC-1, AC-2) |
| `run` | `RunOrchestrator` | Adds the `TRIAGING` step before resolution |

## Prompt resource

`kaizent-server/src/main/resources/prompts/triage.md` uses a small placeholder syntax
(`{{subjects}}`, `{{title}}`, `{{body}}`, `{{labels}}`) rendered by a `PromptTemplate`
helper shared by all prompts. Its content hash is recorded on the run as
`triage.promptVersion`.

Output record:

```java
record TriageOutput(String subjectId, ProblemType problemType, String language, String rationale) {}
```

## Data

`V5__run_triage.sql`: adds `triage jsonb` and `role_id text` to `run`, and an index on
`(triage->>'subjectId')`.

## API

`RunSummary` and `RunDetail` gain `subjectId`, `problemType`, `language`, `roleId` and
`triageRationale`. `GET /runs` gains a `subject` filter.

## Testing

| AC | Test |
|---|---|
| AC-1, AC-2 | Validator tests on fixtures |
| AC-3 to AC-6 | `TriageService` and orchestrator tests with the fake adapter (valid output, unknown subject, failure) |
| AC-7 | API tests |
| AC-8 | Unit test on prompt hashing |
| EV-1 to EV-4 | Evaluation runner (spec 0003) |

## Risks and mitigations

- The model invents subjects → normalize them to `unknown` and record a note (AC-4).
- Labels mislead the classification → the prompt presents labels as unverified hints.
