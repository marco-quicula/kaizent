# Spec 0004: LLM access: API, CLI and fake adapters

- **Status:** Draft
- **Wave:** 1
- **Depends on:** 0002
- **Related ADRs:** 0016, 0017, 0020

## Goal

Complete the model access layer described in ADR-0016:

- the `LlmGateway` and `AgentRuntime` ports;
- configuration profiles per pipeline step;
- Spring AI adapters for Gemini, OpenAI, Anthropic and Ollama;
- CLI adapters through `CliExecutor`, with a validated Codex preset;
- fake adapters;
- usage and cost recording;
- retries.

This lets Kaizent run with a free API tier, with a local CLI subscription, or with fully
scripted fakes. The native agent loop with Kaizent's own tools is implemented in spec
0008, on top of these ports.

## User stories

- As the **maintainer**, I want to run Kaizent through my Codex CLI login, so that I can
  develop without paid API access.
- As a **contributor**, I want to pick any supported API provider through configuration,
  so that I can use what I have.
- As a **test author**, I want scripted fake adapters, so that tests are deterministic and
  offline.

## Acceptance criteria

- **AC-1:** Each pipeline step (`triage`, `resolution`, `lesson`) resolves its adapter
  and model from `kaizent.llm.profiles.<step>` and `kaizent.agent.<step>`. An unknown
  adapter name fails at startup with a clear message.
- **AC-2:** `LlmGateway.complete` returns an instance of the requested Java record type,
  parsed from the model's JSON output and validated against the schema generated from
  that record. On invalid output it makes **one** repair attempt (it sends back the
  validation error), and then fails with `LlmOutputException`.
- **AC-3:** Spring AI adapters exist for `gemini-api`, `openai`, `anthropic` and
  `ollama`. Only adapters whose credentials or URL are configured are activated.
- **AC-4:** Rate-limit and transient errors (HTTP 429 and 5xx) are retried with
  exponential backoff, up to a configurable number of attempts (default 3). Quotas are
  never hard-coded.
- **AC-5:** Every call returns a `Usage` with tokens when they are reported, the
  `costType` (`API`, `SUBSCRIPTION` or `UNKNOWN`), the adapter and the model. For `API`,
  the cost is computed from `kaizent.llm.pricing`. A missing price gives `UNKNOWN`.
- **AC-6:** `CliExecutor` runs a command through `LocalProcessExecutor` with:
  - the prompt passed by file or stdin, as the preset defines;
  - the working directory inside the workspace;
  - a mandatory timeout that kills the whole process tree and raises `TIMEOUT`;
  - stdout and stderr captured, capped in size;
  - a non-zero exit raised as an error, with a truncated stderr excerpt.
- **AC-7:** The CLI `LlmGateway` adapter disables the tool's own tools where the preset
  supports it, and extracts the final JSON answer from the tool's output.
- **AC-8:** The CLI `AgentRuntime` adapter gives the tool read-only access to the
  workspace's `knowledge/` and `skills/` folders, asks for the proposal JSON schema, and
  maps the tool's event stream (when available) to `TraceEvent`s through the `TraceSink`.
- **AC-9:** The `codex` preset is implemented and validated (read-only sandbox, JSON
  events, final message extraction). The `claude-code`, `antigravity`, `gemini-cli` and
  `generic` presets are provided as configuration, documented as "not yet validated".
- **AC-10:** The fake adapters return scripted responses (by step and by ticket key) from
  test resources, and can simulate errors, timeouts and invalid output.
- **AC-11:** API keys and CLI credentials are never logged or stored. The CLI adapters
  never read or copy the tool's credential files.
- **AC-12:** `GET /api/v1/system` lists each pipeline step with its adapter, its model or
  preset, and its cost type.
- **AC-13:** The README documents the CLI mode as a local, personal development mode and
  tells users to check their provider's terms of use (ADR-0016).

## Evaluation cases

- **EV-1:** `codex` preset, `resolution` step, with a skeleton prompt on PC-002 → valid
  proposal JSON, with `costType = SUBSCRIPTION`.
- **EV-2:** `gemini-api`, a triage-shaped request on PC-002 → valid JSON with tokens
  reported.

## Out of scope

- The native agent loop with Kaizent's tools (0008).
- `RemoteRunnerExecutor` and the Kubernetes agent runner (wave 5).
- Exposing Kaizent's tools to CLI tools through MCP (wave 2).

## Open questions

None.
