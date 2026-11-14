# Tasks 0008: Resolution and proposal

- [ ] **T1:** `prompts/resolution.md` and `TaskAssembler`. *Covers:* AC-1.
- [ ] **T2:** `ToolScope` and the three tools, with scope enforcement and the cap on each
  result. *Covers:* AC-2.
- [ ] **T3:** `NativeAgentRuntime` with the tool-call limit, the context check before each
  model call, trace emission and structured final output. *Covers:* AC-3.
- [ ] **T4:** Wire the CLI runtime to the role scope. *Covers:* AC-4.
- [ ] **T5:** `ProposalOutput`, `ProposalValidator` (citations and skills) and
  `ConfidencePolicy`. *Covers:* AC-5 to AC-8.
- [ ] **T6:** `ResolutionService` and the orchestrator integration; remove
  `SkeletonResolver`. *Covers:* AC-9, AC-10.
- [ ] **T8:** The suspicious-ticket policy with its startup warning, and the output
  check (`OutputLeakCheck`, in the `llm` module) with redaction and flags, also wired
  into the triage rationale (spec 0006 AC-3). *Covers:* AC-11, AC-12.
- [ ] **T7:** Run EV-1 to EV-6 with the native runtime and with Codex, and record the
  results in the PR.
