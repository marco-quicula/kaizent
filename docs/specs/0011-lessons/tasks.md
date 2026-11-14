# Tasks 0011: Lessons

- [ ] **T1:** Migration `V7` (`lesson` and `lesson_drafting`), the `Lesson` aggregate and
  its repository.
- [ ] **T2:** `prompts/lesson.md`, `LessonDrafter` (own budget, output check) and
  `LessonDraftingListener` (outcome per verdict, retry of recoverable failures), with trace
  events. *Covers:* AC-1 to AC-5.
- [ ] **T3:** `LessonService` approve (resumable, with the `PUBLISHING` intent and the stable
  file name) and discard, the file writing and the `LessonChanged` events. *Covers:*
  AC-7, AC-8, AC-10.
- [ ] **T4:** The OpenAPI additions and `LessonsController`. *Covers:* AC-6, AC-8.
- [ ] **T5:** The console Lessons page and detail view. *Covers:* AC-6.
- [ ] **T6:** An integration test where an approved lesson is found by search. *Covers:*
  AC-9.
- [ ] **T7:** Manual EV-1 to EV-3, recorded in the PR.
- [ ] **T8:** `FullCycleIT`, the full-cycle test with the fake adapters (the closing
  criterion of wave 1). *Covers:* AC-11.
