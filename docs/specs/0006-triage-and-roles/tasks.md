# Tasks 0006: Triage and roles

- [ ] **T1:** `RoleCatalog`, `FileRoleCatalog` (with `problemTypes`) and the validator
  rules (pair claims, conflicts, conservative default role). *Covers:* AC-1, AC-2.
- [ ] **T2:** The `PromptTemplate` helper (rendering and content hash) and
  `prompts/triage.md`. *Covers:* AC-8.
- [ ] **T3:** `TriageService` with `TriageOutput` and unknown-subject normalization.
  *Covers:* AC-3, AC-4. (The output check on the rationale is wired in with spec 0008 T8,
  where the shared `OutputLeakCheck` is built.)
- [ ] **T4:** `RoleSelector` (subject and type, with the precedence), `RoleEligibility`, the orchestrator `TRIAGING` step, migration `V5` and the
  failure path. *Covers:* AC-5, AC-6.
- [ ] **T5:** API fields and the subject filter. *Covers:* AC-7.
- [ ] **T7:** `InjectionRiskDetector` (deterministic rules), the risk on the run and in
  the API, and synthetic evaluation cases with injected tickets. *Covers:* AC-9.
- [ ] **T6:** Run the evaluation for EV-1 to EV-4 with a real adapter, and record the
  results in the PR.
