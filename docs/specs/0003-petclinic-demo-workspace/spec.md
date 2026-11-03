# Spec 0003: PetClinic demo workspace and evaluation runner

- **Status:** In progress
- **Wave:** 1
- **Depends on:** 0002
- **Related ADRs:** 0013, 0020, 0027, 0028, 0029

## Goal

Provide a realistic, fully synthetic workspace around
[Spring PetClinic](https://github.com/spring-projects/spring-petclinic): subjects, roles,
skills, a knowledge base, 12 tickets and their evaluation cases. Also provide a runner
that executes the evaluation cases against real models. This is the test bed for every
LLM-dependent spec.

## User stories

- As a **contributor**, I want realistic demo data in the repository, so that I can run
  and test Kaizent without access to any real help desk.
- As the **maintainer**, I want to run the evaluation set against a configured model and
  get a report, so that I can tell whether a change to a prompt, skill or adapter made
  answers better or worse.

## Workspace content

**Subjects** (`kaizent-workspace.yaml`):

| Id | Name | Covers |
|---|---|---|
| `owners` | Owners | Registering, finding and editing pet owners |
| `pets` | Pets | Registering and editing pets, pet types |
| `visits` | Visits | Registering visits for pets |
| `technical` | Technical | Running, configuring and errors of the application itself |

**Roles** (`roles/`):

| Role | Subjects | Allowed skills |
|---|---|---|
| `support-agent` (default) | owners, pets, visits | `answer-how-to-question`, `diagnose-reported-error`, `write-customer-reply` |
| `technical-support-engineer` | technical | `diagnose-stack-trace`, `write-customer-reply` |

**Skills** (`skills/<name>/SKILL.md`, Agent Skills format):

- `answer-how-to-question`: find the relevant procedure and answer step by step, citing
  sources.
- `diagnose-reported-error`: tell apart a validation rule or by-design behavior, a known
  issue and a real defect.
- `diagnose-stack-trace`: read a stack trace, find the root exception and match it to
  known issues or setup documentation.
- `write-customer-reply`: tone, structure and language rules for the reply (ADR-0029),
  and never promising features that do not exist.

**Knowledge** (`knowledge/`), 12 files written for this demo. They are consistent with
PetClinic at the commit named in `product/overview.md`, but they are the source of truth for
the demo:

`product/overview.md`, `product/owners.md`, `product/pets.md`, `product/visits.md`,
`product/veterinarians.md`, `faq.md`, `known-issues/KI-001-telephone-validation.md`,
`known-issues/KI-002-future-birth-date.md`, `known-issues/KI-003-error-page-oups.md`,
`setup/running-locally.md`, `setup/databases.md`, `lessons/README.md`.

**Tickets** (`inbox/`) and expected outcomes (`evals/cases/`):

| Ticket | Summary | Subject | Type | Must cite (any of) | Expected |
|---|---|---|---|---|---|
| PC-001 | Owner form rejects telephone `+1 (202) 555-0147` (a number reserved for fiction) | owners | BUG | KI-001 | Explains the format (exactly 10 digits, no formatting) and gives a workaround |
| PC-002 | How to list all owners | owners | HOW_TO | product/owners.md, faq.md | Leave the last name empty and search |
| PC-003 | "Como altero o endereço de um proprietário?" (pt-BR) | owners | HOW_TO | product/owners.md | Steps to edit an owner; **reply in Portuguese** |
| PC-004 | Error adding a pet with a future birth date | pets | BUG | KI-002, product/pets.md | By design (validation), not a defect |
| PC-005 | Cannot add a second pet named "Max" to the same owner | pets | BUG | product/pets.md | Names are unique per owner |
| PC-006 | How to add the pet type "rabbit" | pets | HOW_TO | product/pets.md | No UI for types; needs a data change by an administrator |
| PC-007 | How to register a visit | visits | HOW_TO | product/visits.md | Steps from the owner page |
| PC-008 | Cannot register a visit for today: "Visit date must be in the future" | visits | BUG | product/visits.md | Visits must be in the future by design; the form defaults to tomorrow |
| PC-009 | How to cancel or reschedule a visit | visits | HOW_TO | product/visits.md | Not supported; must **not** invent a cancel feature |
| PC-010 | Stack trace after clicking the "Error" menu item | technical | BUG | KI-003 | The intentional demo error page; not a defect |
| PC-011 | App fails to start with the `postgres` profile: connection refused (stack trace) | technical | BUG | setup/databases.md | Start the database with docker compose; check the profile and credentials |
| PC-012 | Can PetClinic send SMS messages to owners? | owners or `unknown` | OTHER | none | **Low confidence**, lacks information; no invented feature |

## Acceptance criteria

- **AC-1:** The workspace loads without validation errors. Every role references existing
  subjects and skills. Every evaluation case references an existing ticket, and every
  "must cite" path exists.
- **AC-2:** All demo content is in English, except the body of PC-003. All names and
  contacts are synthetic; telephone numbers use ranges reserved for fiction (NANPA
  `555-0100` to `555-0199`). A `README.md` in the workspace says that the data is synthetic.
- **AC-3:** Evaluation cases are YAML files with the properties: `ticket`,
  `expect.subject` (one or a list), `expect.problemType`, `expect.citesAny`,
  `expect.mentionsAll`, `expect.mentionsNone`, `expect.lowConfidence`,
  `expect.ticketLanguage` and `expect.replyLanguage`. Each `mentionsAll` item is a phrase,
  or a list of alternative phrases of which at least one must appear. `mentionsNone` lists
  phrases that must not appear; avoid phrases that a correct reply would use to deny
  something, such as the name of a button that does not exist. These keyword checks are a
  coarse signal: they catch obvious misses but cannot prove that a reply is right, and they
  cannot stop a model from inventing a feature in other words. Semantic checks, for example
  a model acting as a judge, belong to the evaluation harness of wave 4.
- **AC-4:** Running the application with the `eval` profile processes every case through
  the pipeline, without the human review step and without writing to `outbox/`. It then
  writes `target/eval/report-<timestamp>.md`, with pass or fail per property and case,
  totals, tokens, cost and duration, and exits.
- **AC-5:** The evaluation runner refuses to start with the `fake` adapters, unless a
  `--dry-run` flag is set (used by its own tests).
- **AC-6:** The property checkers are unit-tested. `mentionsAll` and `mentionsNone` are
  case-insensitive and run against the reply. `ticketLanguage` compares with the
  language detected by triage, which checks the classification of the input.
  `replyLanguage` detects the language of the generated `suggestedReply` itself, with a
  local language detector and no LLM, and compares it with the expected value. This way
  a triage that is correct cannot hide a reply written in the wrong language.
  `lowConfidence` means an adjusted confidence below 0.5.

## Evaluation cases

The 12 cases above (`EV-PC-001` to `EV-PC-012`). They become meaningful as specs 0006
to 0008 land. Until then, the runner reports the properties that are not yet produced as
"skipped".

## Out of scope

- Using a real PetClinic deployment. The demo needs only the knowledge base.
- Automatic pass or fail thresholds for merging (wave 4).

## Open questions

None.
