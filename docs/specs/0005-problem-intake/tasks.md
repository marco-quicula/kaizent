# Tasks 0005: Problem intake and canonical model

- [ ] **T1:** Refine the `TicketSource` port with `IncomingTicket` variants, and keep the
  model free of file-specific types. *Covers:* AC-9.
- [ ] **T2:** `FileInboxTicketSource`: filtering, size limit, strict UTF-8, front matter
  parsing and defaults, content hash. *Covers:* AC-1, AC-2, AC-3 (detection).
- [ ] **T3:** Migration `V4`, `IntakeIssue` and its repository. *Covers:* AC-3, AC-4
  (persistence).
- [ ] **T4:** `IntakeService`: dedupe, change warnings, id conflicts and ordering.
  *Covers:* AC-4, AC-5, AC-6, AC-7.
- [ ] **T5:** `GET /intake-issues` in the OpenAPI contract and its controller. *Covers:*
  AC-8.
