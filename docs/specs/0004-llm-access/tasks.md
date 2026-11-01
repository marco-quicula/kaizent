# Tasks 0004: LLM access: API, CLI and fake adapters

- [ ] **T1:** Profile configuration (`kaizent.llm.profiles`, `kaizent.agent`) and adapter
  resolution with startup validation. *Covers:* AC-1.
- [ ] **T2:** JSON schema generation from records, validation, and the one-shot repair
  flow in a shared `StructuredOutputSupport`. *Covers:* AC-2.
- [ ] **T3:** Generalize `SpringAiLlmGateway` to `gemini-api`, `openai`, `anthropic` and
  `ollama`, with conditional activation. *Covers:* AC-3.
- [ ] **T4:** Retry with backoff for transient and rate-limit errors. *Covers:* AC-4.
- [ ] **T5:** `Usage`, `CostType` and the pricing table. *Covers:* AC-5.
- [ ] **T6:** `CliExecutor` and `LocalProcessExecutor` (timeouts, process-tree kill,
  output caps, environment allowlist), with fake CLI scripts. *Covers:* AC-6.
- [ ] **T7:** `PresetRegistry`, the `codex` preset, `CliLlmGateway` and the event mapper.
  *Covers:* AC-7, AC-9 (part).
- [ ] **T8:** The `AgentRuntime` port (in `resolution`), `CliAgentRuntime` and
  `TraceSink`. *Covers:* AC-8.
- [ ] **T9:** Fake `LlmGateway` and `AgentRuntime` with scripts and error markers.
  *Covers:* AC-10.
- [ ] **T10:** Unvalidated preset files (`claude-code`, `antigravity`, `gemini-cli`,
  `generic`) and the documentation. *Covers:* AC-9, AC-13.
- [ ] **T11:** Secret hygiene: a log-capture test and the environment allowlist. *Covers:*
  AC-11.
- [ ] **T12:** Add the profiles to `GET /system`. *Covers:* AC-12.
- [ ] **T13:** Manual validation of the Codex preset and of `gemini-api`, recorded in the
  PR. *Covers:* EV-1, EV-2.
