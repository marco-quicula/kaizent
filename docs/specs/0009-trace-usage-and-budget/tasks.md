# Tasks 0009: Run trace, usage and budget

- [ ] **T1:** Migration `V6`, `TraceEvent` and its repository, and `RunTraceSink`.
  *Covers:* AC-1, AC-11.
- [ ] **T2:** The `TracingLlmGateway` decorator, with the payload caps and the redaction
  before persistence. *Covers:* AC-2, AC-6.
- [ ] **T3:** The `TracingAgentRuntime` decorator, native tool-call events and CLI event
  mapping. *Covers:* AC-3, AC-4, AC-5.
- [ ] **T4:** `BudgetGuard` and the `CallBudget` handle used before every provider call
  (repair, retry and loop turn), token estimation, the output limit and timeout
  enforcement. *Covers:* AC-7, AC-8. Also the budget and deadline of the lesson drafting
  (AC-12).
- [ ] **T5:** `RunRetryService` and the retry endpoint. *Covers:* AC-9.
- [ ] **T6:** The trace endpoint, and usage and budget in `RunDetail`. *Covers:* AC-10.
