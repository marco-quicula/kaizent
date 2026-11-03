# Tasks 0003: PetClinic demo workspace and evaluation runner

- [x] **T1:** Write `kaizent-workspace.yaml`, the two roles and the workspace
  `README.md`. *Covers:* AC-2 (part).
- [x] **T2:** Write the 12 knowledge files. *Done when:* the maintainer has reviewed them
  for consistency. *Covers:* AC-2.
- [x] **T3:** Write the four skills in the Agent Skills format. *Covers:* AC-1 (part).
- [x] **T4:** Write the 12 tickets and the 12 evaluation cases. *Covers:* AC-3.
- [ ] **T5:** `WorkspaceLoader` and `WorkspaceValidator`, with tests on the real
  workspace and on broken fixtures. *Covers:* AC-1.
- [ ] **T6:** `ReviewMode` and the `EVALUATED` status (migration `V3`), with the
  orchestrator skipping review and delivery. *Done when:* the orchestrator test passes.
- [ ] **T7:** `PropertyChecker`s with unit tests, including `ReplyLanguageChecker`
  (Lingua, run on `suggestedReply`) separate from the `ticketLanguage` check. *Covers:*
  AC-6.
- [ ] **T8:** `EvalRunner` and `EvalReportWriter`, including the dry-run mode and the
  refusal to run with fake adapters. *Covers:* AC-4, AC-5.
- [ ] **T9:** README section "Running evaluations". *Done when:* the maintainer has run it
  once with a real adapter.
