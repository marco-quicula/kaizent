# Tasks 0008: Resolution and proposal

- [ ] **T1:** `prompts/resolution.md` and `TaskAssembler`. *Covers:* AC-1.
- [ ] **T2:** `ToolScope` and the three tools, with scope enforcement and the cap on each
  result. *Covers:* AC-2.
- [ ] **T3:** `NativeAgentRuntime` with the tool-call limit, the context check before each
  model call, trace emission and structured final output. *Covers:* AC-3.
- [ ] **T4:** *Moved to wave 2 (ADR-0033):* wire the CLI runtime to the role scope.
  *Covers:* AC-4 (moved).
- [ ] **T5:** `ProposalOutput`, `ProposalValidator` (citations and skills) and
  `ConfidencePolicy`. *Covers:* AC-5 to AC-8.
- [ ] **T6:** `ResolutionService` and the orchestrator integration; remove
  `SkeletonResolver`. *Covers:* AC-9, AC-10.
- [ ] **T8:** The wave 1 part of the suspicious-ticket handling (the risk shown, `resolvedBy`,
  the refusal of `runtime: cli`), and the output
  check (`OutputLeakCheck`, in the `llm` module) with redaction and flags, also wired
  into the triage rationale (spec 0006 AC-3). *Covers:* AC-11, AC-12.
- [ ] **T7:** Run EV-1 to EV-6 with the native runtime (an API profile), and record the
  results in the PR.
