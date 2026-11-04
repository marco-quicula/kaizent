# Spec 0006: Triage and roles

- **Status:** Approved
- **Wave:** 1
- **Depends on:** 0003, 0004, 0005
- **Related ADRs:** 0016, 0023, 0029

## Goal

Add the Plan step: classify each problem's subject, problem type and language with one
structured model call, and select the role that will handle it, using the roles defined
in the workspace.

## User stories

- As a **reviewer**, I want each run to show its subject, type, language and role, with a
  short rationale, so that I can see how the agent framed the problem.
- As a **workspace owner**, I want to define roles in Markdown files, so that I control
  personas, skills and knowledge per subject without changing code.

## Acceptance criteria

- **AC-1:** The roles in `roles/*.md` are loaded at startup with `id`, `name`,
  `subjects`, `allowedSkills`, `knowledgePaths` and the persona (the body). Invalid roles
  fail workspace validation with the file and the reason.
- **AC-2:** Every subject in `kaizent-workspace.yaml` is covered by at most one role.
  Subjects without a role fall back to `defaultRole`, which must exist.
- **AC-3:** A `RECEIVED` run moves to `TRIAGING`. One `LlmGateway` call (profile
  `triage`) receives the problem's title, body and labels (marked as hints) and the list
  of subjects (id, name, description). It returns a `TriageResult` with `subjectId` (a
  declared id or `unknown`), `problemType` (`BUG`, `HOW_TO` or `OTHER`), `language` (ISO
  639-1) and `rationale` (English, at most 300 characters).
- **AC-4:** A `subjectId` that is not declared is treated as `unknown`, with a recorded
  note.
- **AC-5:** The role is selected deterministically: the role whose `subjects` contain the
  subject, otherwise the default role. The triage result and the role id are stored on
  the run, and the run moves on to `RESOLVING`.
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
  such as `curl`, `wget` or reading home-folder files; long encoded blobs; URLs outside
  the workspace's known hosts. The risk and the reasons are stored on the run and shown by
  `GET /runs/{id}`. Synthetic evaluation cases with injected tickets check the detector.

## Evaluation cases

- **EV-1:** For PC-001 to PC-011, the subject equals the expected subject (target: at
  least 10 of 11).
- **EV-2:** For all 12 tickets, the problem type equals the expected type (target: at
  least 10 of 12).
- **EV-3:** PC-003 → `language = pt`; all others → `en`.
- **EV-4:** PC-012 → `owners` or `unknown`.

The targets are reported, not enforced, in wave 1.

## Out of scope

- Several roles per subject, or role escalation (later waves).
- Choosing the role with the model. The selection is deterministic by design.

## Open questions

None.
