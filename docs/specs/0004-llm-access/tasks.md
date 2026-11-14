# Tasks 0004: LLM access: API, CLI and fake adapters

- [ ] **T1:** Profile configuration (`kaizent.llm.profiles`, `kaizent.agent`) and adapter
  resolution with startup validation. *Covers:* AC-1.
- [x] **T2:** JSON schema generation from records, validation, and the one-shot repair
  flow in a shared `StructuredOutputs`. *Covers:* AC-2.
- [x] **T3:** Generalize `SpringAiLlmGateway` to `gemini-api`, `openai`, `anthropic` and
  `ollama`, with conditional activation. *Covers:* AC-3.
- [x] **T4:** Retry with backoff for transient and rate-limit errors, honoring a suggested
  wait, and keeping the provider's cause in the error. *Covers:* AC-4.
- [x] **T5:** `Usage`, `CostType` and the pricing table. *Covers:* AC-5.
- [x] **T6:** `CliExecutor` and `DockerCliExecutor` (container contract, timeouts that stop
  the container, output caps, environment allowlist), with a fake CLI image. *Covers:*
  AC-6.
- [x] **T7:** The presets (configuration `kaizent.cli.presets`, see the plan), the `codex`
  preset and image, the `cli` compose profile with its login command, `CliLlmGateway` (the
  `cli` adapter) and the event reader. *Covers:* AC-7, AC-9 (part), AC-11 (part). The
  preset became `validated: true` with EV-1 (T13).
- [ ] **T8:** The `AgentRuntime` port (in `resolution`) and the `TraceSink`. *Covers:*
  AC-8. The `CliAgentRuntime`, the read and network restrictions of ADR-0032 (layer 1)
  and the Codex helper-binaries check (`/tmp`, `tmpfs`) moved to wave 2 (ADR-0033). The
  fake `AgentRuntime` is in T9.
- [ ] **T9:** Fake `LlmGateway` and `AgentRuntime` with scripts and error markers.
  *Covers:* AC-10. (The fake `LlmGateway` is done: scripts by step and ticket key, and the
  `__error__`, `__timeout__` and `__invalid__` markers. The fake `AgentRuntime` comes with
  the `AgentRuntime` port, after the review of the resolution flow.)
- [ ] **T10:** Unvalidated presets and images (`claude-code`, `antigravity`, `gemini-cli`),
  the `generic` preset, and the documentation of the CLI mode and its setup. *Covers:*
  AC-9, AC-13. (The README section on the CLI mode, its terms-of-use notice and the Codex
  setup are done; the other presets and images remain.)
- [x] **T11:** Secret hygiene: a log-capture test and the environment allowlist. *Covers:*
  AC-11. (The API adapters' log-capture and redaction tests came with #38; the
  environment allowlist and the redacted error excerpts of the CLI executor with T6.)
- [x] **T12:** Add the profiles to `GET /system`. *Covers:* AC-12.
- [x] **T13:** Manual validation of the Codex preset and of `gemini-api`, recorded in the
  PR, and optionally of `openai` and `anthropic`. *Covers:* EV-1, EV-2, EV-3 (optional).
  The `llm-check` command runs the triage-shaped requests. Results (#44): EV-1 passed
  with Codex CLI 0.166.0 in the full pipeline (12 of 12 tickets in `AWAITING_REVIEW`,
  `costType = SUBSCRIPTION`, the login volume still holds only `auth.json`); EV-2 passed
  with `gemini-api` and `gemini-3.6-flash` (valid triage-shaped JSON, tokens reported).
  EV-3 is partly validated: three OpenAI models in #38, with the resolution call;
  Anthropic and the triage-shaped request were not run (optional).
