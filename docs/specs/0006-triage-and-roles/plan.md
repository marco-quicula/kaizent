# Plan 0006: Triage and roles

## Modules and components

| Module | Component | Responsibility |
|---|---|---|
| `triage` | `RoleCatalog` (port), `FileRoleCatalog` | Loads `roles/*.md` (front matter and persona) |
| `triage` | `RoleSelector` | Deterministic mapping (subject, problem type) → role, inside the eligible set (ADR-0034): exact pair, then subject only, then the default role. It returns the role and how it matched |
| `triage` | `TriageService` | Builds the prompt from `prompts/triage.md`, calls `LlmGateway` (step `triage`) with `TriageOutput`, normalizes unknown subjects, and passes the rationale through the shared `OutputLeakCheck` (module `llm`, spec 0008) before storing it |
| `workspace` | `WorkspaceValidator` | Extended with role rules (AC-1, AC-2): the claim of each (subject, type) pair, conflicts between roles, and the conservative scope of the default role |
| `run` | `RunOrchestrator` | Adds the `TRIAGING` step before resolution |
| `triage` | `RoleEligibility` | Returns the roles that are eligible for a ticket, from its origin and the workspace policy. Wave 1: every role. It is the seam where wave 2 adds trusted metadata (ADR-0034) |
| `triage` | `InjectionRiskDetector` | Deterministic injection-risk rules (AC-9, ADR-0032 layer 2): returns `SAFE` or `SUSPICIOUS`, the matched reasons and the notes. It runs before the triage call and costs no model call |

## Role file format

```markdown
---
id: technical-support-engineer
name: Technical support engineer
subjects: [technical]
problemTypes: [BUG, OTHER]     # optional; omitted = BUG, HOW_TO and OTHER
allowedSkills: [diagnose-stack-trace, write-customer-reply]
knowledgePaths: [setup, known-issues, faq.md, lessons]
---
Persona text...
```

Selection for the pair (subject, type): the role that lists both; else the role that lists
the subject and no types; else `defaultRole`. The existing PetClinic roles omit
`problemTypes`, so they stay valid and keep their behavior.

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
`detectorVersion`) and `roleMatch` (`SUBJECT_AND_TYPE`, `SUBJECT` or `DEFAULT`).

## API

`RunSummary` and `RunDetail` gain `subjectId`, `problemType`, `language`, `roleId` and
`triageRationale`. `GET /runs` gains a `subject` filter. `RunSummary` also gains
`injectionRisk` (`SAFE` or `SUSPICIOUS`), and `RunDetail` gains the reasons and the notes
(contract first, ADR-0024).

## Testing

| AC | Test |
|---|---|
| AC-1, AC-2 | Validator tests on fixtures: a role with types, a role without, two roles claiming the same pair (error naming both), a type-specific role next to a general one on the same subject (valid), a default role with a wildcard path (error) |
| AC-5 | `RoleSelector` tests: exact pair beats subject only; subject only beats default; `unknown` and no match go to the default role; labels and the rationale never change the role |
| AC-3 to AC-6 | `TriageService` and orchestrator tests with the fake adapter (valid output, unknown subject, failure, a rationale that leaks a synthetic token: redacted and flagged) |
| AC-7 | API tests |
| AC-8 | Unit test on prompt hashing |
| AC-9 | `InjectionRiskDetector` unit tests: each rule with positive and negative synthetic samples, including Portuguese phrasings; the 12 demo tickets are `SAFE`; URLs alone give notes, not reasons; orchestrator test that the risk is stored before the triage call; API test |
| EV-1 to EV-5 | Evaluation runner (spec 0003); the injected tickets of EV-5 are evaluation cases only |

## Risks and mitigations

- The model invents subjects → normalize them to `unknown` and record a note (AC-4).
- Labels mislead the classification → the prompt presents labels as unverified hints.
- The deterministic rules flag legitimate tickets (false positives) → in wave 1 the ticket
  is resolved like any other by the native runtime (spec 0008 AC-11), and the reasons are
  shown to the reviewer; the rules are tuned with evaluation cases.
- Injections the rules do not recognize (false negatives) → the rules are only layer 2;
  layer 1 (least capability) keeps an unrecognized injection harmless (ADR-0032).
