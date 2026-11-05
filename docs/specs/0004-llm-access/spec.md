# Spec 0004: LLM access: API, CLI and fake adapters

- **Status:** In progress
- **Wave:** 1
- **Depends on:** 0002
- **Related ADRs:** 0016, 0017, 0020, 0030

## Goal

Complete the model access layer described in ADR-0016:

- the `LlmGateway` and `AgentRuntime` ports;
- configuration profiles per pipeline step;
- Spring AI adapters for Gemini, OpenAI, Anthropic and Ollama;
- CLI adapters through `CliExecutor`, which runs each CLI call in a new container
  (ADR-0030), with a validated Codex preset;
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
  `ollama`. An adapter is usable only when its key (or, for `ollama`, its URL) is
  configured. The application still starts without them: a step that uses an
  unconfigured adapter logs a warning at startup, and its calls fail with a message that
  names the missing variable.
- **AC-4:** Rate-limit and transient errors (HTTP 429 and 5xx) are retried with
  exponential backoff, up to a configurable number of attempts (default 3), and a wait
  suggested by the provider is respected. A suggested wait longer than a configurable
  maximum (default 60 s, for example a daily quota) is not waited for: the call fails at
  once. Quotas are never hard-coded. A failed call keeps
  the provider's cause: the HTTP status, the provider's error code and message, and the
  suggested wait when given. The run's failure message and the evaluation report show it,
  instead of a generic "failed to generate content".
- **AC-5:** Every call returns a `Usage` with tokens when they are reported, the
  `costType` (`API`, `SUBSCRIPTION` or `UNKNOWN`), the adapter and the model. For `API`,
  the cost is computed from `kaizent.llm.pricing`. A missing price gives `UNKNOWN`.
- **AC-6:** `CliExecutor` runs each command in a new container through
  `DockerCliExecutor` (ADR-0030), with:
  - the preset's image, run as a non-root user, with configurable CPU and memory limits;
  - only the workspace paths that the call grants, mounted read-only at their relative
    paths under the working directory, so that nothing else of the workspace (including
    `evals/`) is visible; a writable scratch folder per call; and the tool's credential
    volume, used for the login only; no other host path and never the Docker socket;
  - only the environment variables that the preset declares;
  - the prompt passed by file or stdin, as the preset defines;
  - a mandatory timeout that stops the container and raises `TIMEOUT`;
  - stdout and stderr captured, capped in size;
  - a non-zero exit raised as an error, with a truncated stderr excerpt;
  - a clear error when Docker is not available.
- **AC-7:** The CLI `LlmGateway` adapter disables the tool's own tools where the preset
  supports it, grants no workspace path, and extracts the final JSON answer from the
  tool's output.
- **AC-8:** The CLI `AgentRuntime` adapter gives the tool read-only access only to the
  task's knowledge roots and skill folders (the role's, as spec 0008 AC-4 requires), asks
  for the proposal JSON schema, and
  maps the tool's event stream (when available) to `TraceEvent`s through the `TraceSink`.
  It may **read** only the granted paths and its scratch folder, not the tool's login nor
  any other file, and its network is limited to what the tool needs to reach its model,
  where the tool or the container allows it (ADR-0032, layer 1). It is released only once
  this is validated with the real tool. A tool that cannot restrict reads has its agentic
  preset marked `read-restricted: false`; a documented warning does not lift the gate, and
  such a preset is never used to resolve a ticket, whatever its injection risk: the
  application refuses to start with it as a resolution runtime (spec 0008 AC-11).
- **AC-9:** The `codex` preset and its image are implemented and validated (read-only
  sandbox, JSON events, final message extraction). The `claude-code`, `antigravity` and
  `gemini-cli` presets and images, and the `generic` preset, are provided and documented
  as "not yet validated". Each image pins its tool's version and is built locally with
  `docker compose` (profile `cli`).
- **AC-10:** The fake adapters return scripted responses (by step and by ticket key) from
  test resources, and can simulate errors, timeouts and invalid output.
- **AC-11:** API keys and CLI credentials are never logged or stored. Each CLI tool keeps
  its login in its own Docker volume, filled by the tool's own login command run in a
  container; Kaizent mounts that volume but never reads or copies it. An API key for a
  CLI tool reaches only that tool's container.
- **AC-12:** `GET /api/v1/system` lists each pipeline step with its adapter, its model or
  preset, and its cost type.
- **AC-13:** The README documents the CLI mode as a local, personal development mode and
  tells users to check their provider's terms of use (ADR-0016). It documents the setup:
  building the images and logging in to each tool (ADR-0030).

## Evaluation cases

- **EV-1:** `codex` preset, `resolution` step, with a skeleton prompt on PC-002 → valid
  proposal JSON, with `costType = SUBSCRIPTION`.
- **EV-2:** `gemini-api`, a triage-shaped request on PC-002 → valid JSON with tokens
  reported.
- **EV-3 (optional):** the same request with `openai` and with `anthropic`, when the
  maintainer has keys for them. It is run in addition to EV-2 and does not block the
  spec.

## Out of scope

- The native agent loop with Kaizent's tools (0008).
- `RemoteRunnerExecutor` and the Kubernetes agent runner (wave 5). It will run the same
  images under the same container contract (ADR-0030).
- Falling back to other models when a quota runs out (open decision 18).
- Exposing Kaizent's tools to CLI tools through MCP (wave 2).

## Open questions

None.
