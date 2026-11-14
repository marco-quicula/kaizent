# Spec 0006: Triage and roles

- **Status:** Approved
- **Wave:** 1
- **Depends on:** 0003, 0004, 0005
- **Related ADRs:** 0016, 0023, 0029, 0033, 0034

## Goal

Add the Plan step: classify each problem's subject, problem type and language with one
structured model call, and have the code select the role that will handle it, from the
roles defined in the workspace.

## User stories

- As a **reviewer**, I want each run to show its subject, type, language and role, with a
  short rationale, so that I can see how the agent framed the problem.
- As a **workspace owner**, I want to define roles in Markdown files, so that I control
  personas, skills and knowledge per subject without changing code.

## Acceptance criteria

- **AC-1:** The roles in `roles/*.md` are loaded at startup with `id`, `name`,
  `subjects`, `problemTypes` (optional: a list of `BUG`, `HOW_TO` and `OTHER`; omitted
  means all three), `allowedSkills`, `knowledgePaths` and the persona (the body). Invalid
  roles fail workspace validation with the file and the reason.
- **AC-2:** A role serves the pairs (subject, problem type) of its `subjects` and
  `problemTypes`. A role claims in one of two **tiers** (the tiers of the precedence in
  AC-5): an **exact** claim for each pair it lists, when it states `problemTypes`, and a
  **subject-wide** claim for each subject it lists, when it omits `problemTypes`. Within a
  tier, at most one role may claim the same pair (exact) or the same subject
  (subject-wide): two roles that do fail validation, and the message names both. Roles in
  different tiers never conflict, so a subject-wide role plus a role limited to
  `problemTypes: [BUG]` on the same subject is valid, and the exact claim wins for
  its pairs (AC-5). Pairs with no role fall back to `defaultRole`, which
  must exist and have a **conservative scope**: its `knowledgePaths` must name explicit
  paths (not the whole `knowledge/` tree), because the code assigns it whenever nothing
  matches, including for the subject `unknown`. Validation reports the pairs that only the
  default role covers.
- **AC-3:** A `RECEIVED` run moves to `TRIAGING`. One `LlmGateway` call (profile
  `triage`) receives the problem's title, body and labels (marked as hints) and the list
  of subjects (id, name, description). The labels never enter the role selection (AC-5).
  It returns a `TriageResult` with `subjectId` (a
  declared id or `unknown`), `problemType` (`BUG`, `HOW_TO` or `OTHER`), `language` (ISO
  639-1) and `rationale` (English, at most 300 characters). The rationale is model text
  that is stored and shown, so it passes the **output check** of spec 0008 AC-12 (ADR-0032,
  layer 4) before it is stored; a redaction is flagged on the triage result.
- **AC-4:** A `subjectId` that is not declared is treated as `unknown`, with a recorded
  note.
- **AC-5:** The role is selected **by code**, deterministically, from the subject and the
  problem type of the triage result (ADR-0034), within the roles that are eligible for the
  ticket. In wave 1 every role of the workspace is eligible (the only source is the local
  file inbox), but the selector takes the eligible set from code so that wave 2 can narrow
  it by origin and policy. The precedence is:
  1. the role that lists the subject **and** the problem type;
  2. otherwise, the role that lists the subject and omits `problemTypes`;
  3. otherwise (no match, or the subject is `unknown`), the default role.

  The model never names a role, and the ticket's labels are never an input. The triage
  result, the role id and how it matched (`SUBJECT_AND_TYPE`, `SUBJECT` or `DEFAULT`) are
  stored on the run, and the run moves on to `RESOLVING`.
- **AC-6:** A triage failure (after the retries and the repair attempt of spec 0004)
  moves the run to `FAILED` with reason `ERROR`.
- **AC-7:** `GET /api/v1/runs` and `GET /api/v1/runs/{id}` include the subject, problem
  type, language, role and triage rationale, and `GET /runs` can filter by subject.
- **AC-8:** The triage prompt is a versioned resource file (`prompts/triage.md`) in
  English, and the run records which prompt version (content hash) was used.
- **AC-9:** Every ticket gets an **injection risk**, `SAFE` or `SUSPICIOUS`, with the
  matched reasons, from a deterministic detector that runs before the triage call and
  costs no model call (ADR-0032, layer 2): instructions to ignore or reveal instructions;
  requests for files, environment variables, tokens, keys or passwords; shell commands
  such as `curl`, `wget` or reading home-folder files; long encoded blobs. A URL to a host
  outside `kaizent.security.known-hosts` is recorded as a note, not as a reason on its
  own. The risk, the reasons and the notes are stored on the run and shown by
  `GET /runs/{id}`. Synthetic evaluation cases with injected tickets check the detector.

## Evaluation cases

- **EV-1:** For PC-001 to PC-011, the subject equals the expected subject (target: at
  least 10 of 11).
- **EV-2:** For all 12 tickets, the problem type equals the expected type (target: at
  least 10 of 12).
- **EV-3:** PC-003 → `language = pt`; all others → `en`.
- **EV-4:** PC-012 → `owners` or `unknown`.
- **EV-4b:** the role follows the mapping: PC-010 and PC-011 → `technical-support-engineer`;
  PC-001 → `support-agent`; a ticket whose subject is `unknown` → the default role, with
  `roleMatch = DEFAULT`.
- **EV-5:** The 12 PetClinic tickets → `SAFE`; synthetic injected tickets (instructions to
  ignore the rules, requests for tokens or files, shell commands) → `SUSPICIOUS` with the
  matching reasons. The injected tickets are evaluation cases only; they never enter the
  demo inbox.

The targets are reported, not enforced, in wave 1.

## Out of scope

- Several roles per subject, or role escalation (later waves).
- Choosing the role with the model. The selection is deterministic by design.
- The triage modes `llm`, `rules` and `rules-then-llm` (`kaizent.triage.mode`), where a
  rule or the model chooses among the eligible candidates: wave 2, when a real connector
  gives trusted metadata with its provenance (ADR-0034). In wave 1 the LLM classifies and
  the code maps.

## Open questions

None.
