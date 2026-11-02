# Tasks 0002: Walking skeleton

- [x] **T1:** `workspace` module: properties and path resolution, with a minimal
  `examples/petclinic/` containing `inbox/PC-001.md` and an empty `outbox/.gitkeep`.
  *Done when:* a unit test resolves the paths.
- [x] **T2:** Flyway `V2`, `Problem` and `Run` aggregates, Spring Data JDBC repositories.
  *Done when:* the repository integration tests pass.
- [x] **T3:** `TicketSource` port, `FileInboxTicketSource` and `IntakePoller` with
  idempotent creation. *Done when:* the AC-1 and AC-2 tests pass. *Covers:* AC-1, AC-2.
- [x] **T4:** `LlmGateway` port, `FakeLlmGateway` and the minimal `SpringAiLlmGateway`
  (`gemini-api`). *Done when:* a fake-based unit test returns a typed `Proposal`.
- [x] **T5:** `RunWorker` and `RunOrchestrator` with `SkeletonResolver`, including
  failure handling. *Done when:* the AC-3 and AC-4 tests pass. *Covers:* AC-3, AC-4.
- [x] **T6:** OpenAPI additions, `RunsController`, `ReviewService` (`APPROVED` only),
  `ReplySink` and `FileOutboxReplySink`. *Done when:* the API tests pass. *Covers:*
  AC-5, AC-6, AC-7.
- [x] **T7:** Console `RunsPage` and `RunDetailPage` with the Approve button. *Done
  when:* the Vitest tests pass. *Covers:* AC-8.
- [x] **T8:** End-to-end test with the fake adapter. *Done when:* it is green in CI.
  *Covers:* AC-9.
- [x] **T9:** Manual run with `gemini-api`, documented in the README ("Run with a real
  model"). *Covers:* AC-10, EV-1.
