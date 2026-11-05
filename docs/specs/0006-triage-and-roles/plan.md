# Plan 0006: Triage and roles

## Modules and components

| Module | Component | Responsibility |
|---|---|---|
| `triage` | `RoleCatalog` (port), `FileRoleCatalog` | Loads `roles/*.md` (front matter and persona) |
| `triage` | `RoleSelector` | Deterministic subject → role mapping |
| `triage` | `TriageService` | Builds the prompt from `prompts/triage.md`, calls `LlmGateway` (step `triage`) with `TriageOutput`, normalizes unknown subjects |
| `workspace` | `WorkspaceValidator` | Extended with role rules (AC-1, AC-2) |
| `run` | `RunOrchestrator` | Adds the `TRIAGING` step before resolution |
| `triage` | `InjectionRiskDetector` | Deterministic injection-risk rules (AC-9, ADR-0032 layer 2): returns `SAFE` or `SUSPICIOUS`, the matched reasons and the notes. It runs before the triage call and costs no model call |

## Prompt resource

`kaizent-server/src/main/resources/prompts/triage.md` uses a small placeholder syntax
(`{{subjects}}`, `{{title}}`, `{{body}}`, `{{labels}}`) rendered by a `PromptTemplate`
helper shared by all prompts. Its content hash is recorded on the run as
`triage.promptVersion`.

Output record:

```java
record TriageOutput(String subjectId, ProblemType problemType, String language, String rationale) {}
```

## Injection risk (AC-9)

The rules live in code, each with an id, a pattern and a test:

| Rule id | Matches (case-insensitive, after Unicode normalization) | Effect |
|---|---|---|
| `instruction-override` | "ignore / disregard / forget … (previous / above / your) instructions / rules / prompt", "you are now", "system prompt", "reveal / print your instructions" | reason |
| `secret-request` | requests to show or send tokens, keys, passwords, credentials, `auth.json`, `.env`, environment variables | reason |
| `file-request` | absolute paths or home-folder paths (`/etc/`, `~/`, `/home/`, `C:\Users\`), "read / cat / open … file" | reason |
| `shell-command` | `curl`, `wget`, `nc`, `bash -c`, `powershell`, `$(…)`, backticks with a command | reason |
| `encoded-blob` | a base64 or hex run of 200 characters or more | reason |
| `unknown-host` | a URL whose host is not in `kaizent.security.known-hosts` (default empty) | note only |

A ticket with at least one reason is `SUSPICIOUS`. The detector has a version (the hash of
its rule set), recorded with the result, so that evaluation results can be compared over
time.

## Data

`V5__run_triage.sql`: adds `triage jsonb` and `role_id text` to `run`, and an index on
`(triage->>'subjectId')`. The `triage` document also holds `injectionRisk`
(`level`, `reasons[]` with the rule id and the matched excerpt, `notes[]`,
`detectorVersion`).

## API

`RunSummary` and `RunDetail` gain `subjectId`, `problemType`, `language`, `roleId` and
`triageRationale`. `GET /runs` gains a `subject` filter. `RunSummary` also gains
`injectionRisk` (`SAFE` or `SUSPICIOUS`), and `RunDetail` gains the reasons and the notes
(contract first, ADR-0024).

## Testing

| AC | Test |
|---|---|
| AC-1, AC-2 | Validator tests on fixtures |
| AC-3 to AC-6 | `TriageService` and orchestrator tests with the fake adapter (valid output, unknown subject, failure) |
| AC-7 | API tests |
| AC-8 | Unit test on prompt hashing |
| AC-9 | `InjectionRiskDetector` unit tests: each rule with positive and negative synthetic samples, including Portuguese phrasings; the 12 demo tickets are `SAFE`; URLs alone give notes, not reasons; orchestrator test that the risk is stored before the triage call; API test |
| EV-1 to EV-5 | Evaluation runner (spec 0003); the injected tickets of EV-5 are evaluation cases only |

## Risks and mitigations

- The model invents subjects → normalize them to `unknown` and record a note (AC-4).
- Labels mislead the classification → the prompt presents labels as unverified hints.
- The deterministic rules flag legitimate tickets (false positives) → the default policy
  only routes them to the API loop (spec 0008 AC-11), which still resolves them; reasons
  are shown to the reviewer, and the rules are tuned with evaluation cases.
- Injections the rules do not recognize (false negatives) → the rules are only layer 2;
  layer 1 (least capability) keeps an unrecognized injection harmless (ADR-0032).
