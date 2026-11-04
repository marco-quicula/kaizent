# Plan 0004: LLM access: API, CLI and fake adapters

## Ports

```java
// llm module (public API)
interface LlmGateway {
  <T> LlmResponse<T> complete(LlmRequest<T> request);
}
record LlmRequest<T>(String step, String system, String user, Class<T> outputType,
                     Map<String, Object> attributes) {}
record LlmResponse<T>(T output, Usage usage, String rawOutput) {}

interface CliExecutor {
  CliResult execute(CliCommand command);   // preset, args, stdin or prompt file, granted read-only paths, timeout
}

// resolution module (public API); the adapters live in llm/adapter/cli and resolution/adapter/*
interface AgentRuntime {
  AgentResult run(AgentTask task, TraceSink trace);
}
record AgentTask(String step, String instructions, String problemText,
                 List<SkillSummary> skills, List<Path> knowledgeRoots, List<Path> skillRoots,
                 JsonSchema outputSchema, Budget budget) {}
record AgentResult(String outputJson, Usage usage, List<String> rawEvents) {}
```

`AgentRuntime` belongs to the `resolution` module, because it describes a resolution
task. The CLI implementation reuses `CliExecutor` from the `llm` module.

## Adapters

| Name | Port(s) | Notes |
|---|---|---|
| `gemini-api`, `openai`, `anthropic`, `ollama` | `LlmGateway` | Spring AI `ChatClient`, structured output through Spring AI's `BeanOutputConverter` (or the provider's native JSON schema mode when available), with our schema validation on top |
| `cli` + preset | `LlmGateway`, `AgentRuntime` | `PresetRegistry` loads presets from `classpath:cli-presets/*.yaml`, overridable through `kaizent.cli.presets.*` |
| `fake` | `LlmGateway`, `AgentRuntime` | `FakeScript` loaded from `classpath:fake/<step>/<ticket-key>.json`, with fallback to `default.json`; special markers `__error__`, `__timeout__`, `__invalid__` |

**Preset shape** (YAML). The exact flags are verified with the tool's `--help`, inside
its image, during implementation:

```yaml
name: codex
image: kaizent-cli-codex       # built from docker/cli/codex/Dockerfile (ADR-0030)
toolVersion: <pinned version>  # the version the preset was validated with
credentials:                   # the tool's login volume and where the tool keeps its login
  volume: kaizent-cli-codex
  path: /home/kaizent/.codex   # sessions and history are kept out of it (ephemeral mode,
                               # or redirected to /scratch); checked when validating
env: []                        # variables passed from .env, for tools that use API keys
command: codex
llm:   # single structured call, with tools disabled
  args: [exec, --json, --sandbox, read-only, --skip-git-repo-check, --output-schema, "{schemaFile}", "-"]
  prompt: stdin
  output: { format: jsonl-events, finalMessage: last-agent-message }
agent: # agentic task
  args: [exec, --json, --sandbox, read-only, --skip-git-repo-check, --cd, "{workspace}", --output-schema, "{schemaFile}", "-"]
  prompt: stdin
  output: { format: jsonl-events, finalMessage: last-agent-message, events: codex }
costType: SUBSCRIPTION
validated: true
```

**Event mapping:** a per-preset `EventMapper` turns JSONL events into `TraceEvent`s (tool
call, file read, message). Unknown events are kept raw.

**`DockerCliExecutor` (ADR-0030):** builds a `docker run --rm` command line and runs it
with `ProcessBuilder`:

- a unique container name per call, so that a timeout can `docker kill` it;
- `--read-only` root file system with a `--tmpfs /tmp` (a preset can declare more
  temporary paths that its tool needs), `--user` non-root, `--cpus` and `--memory` from
  `kaizent.cli.limits`;
- only the paths that the `CliCommand` grants, each mounted read-only at its relative
  path (`-v <workspace>/knowledge/product:/workspace/knowledge/product:ro`, and so on),
  with `-w /workspace`, so that citations keep their workspace-relative paths and nothing
  else of the workspace is visible; a per-call scratch folder mounted writable at
  `/scratch` for the prompt, the schema file, output files and the tool's other state;
  and the preset's credential volume at the preset's path;
- only the preset's `env` variables (`-e NAME`, with values from the environment or
  `.env`), never the whole host environment;
- the prompt written to stdin or to `/scratch`, as the preset defines, and stdin closed
  afterwards;
- capped output buffers, and a clear error when `docker` is missing or the daemon is not
  running.

**Images and setup:** `docker/cli/<tool>/Dockerfile` installs the tool at a pinned
version on an official base image and adds a non-root user. `compose.yaml` gets a `cli`
profile with one service per tool, so that `docker compose --profile cli build` builds
the images and `docker compose run --rm <tool>-cli <login command>` fills the tool's
credential volume. The exact login command of each tool is verified during
implementation and documented in the README.

## Data

No new tables. `Usage` is persisted per trace event in spec 0009. In this spec, the
run's proposal JSON carries the usage totals.

## Configuration

```yaml
kaizent:
  llm:
    profiles:
      triage:     { adapter: gemini-api, model: <id> }
      lesson:     { adapter: gemini-api, model: <id> }
    retry: { max-attempts: 3, initial-backoff: 2s, multiplier: 2 }
    pricing:
      <model-id>: { input-per-million: 0.0, output-per-million: 0.0 }
  agent:
    resolution: { runtime: cli, preset: codex, timeout: 5m }
  cli:
    max-output: 2MB
    limits: { cpus: 2, memory: 2g }
```

## Testing

| AC | Test |
|---|---|
| AC-1 | Context tests with valid and invalid profiles |
| AC-2 | Unit tests with a stubbed `ChatModel`: valid output, invalid then repaired, invalid twice |
| AC-3 | Conditional activation tests |
| AC-4 | Retry tests with a stub that fails with 429 and then succeeds, honors a suggested wait, and keeps the provider's cause when it gives up |
| AC-5 | Pricing computation tests |
| AC-6 to AC-8 | `DockerCliExecutor` and CLI adapter tests against a fake CLI image built from `src/test/resources/fake-cli/` (prints canned JSONL, sleeps for timeouts, exits non-zero, tries to write to the workspace, and tries to read paths it was not granted, including `evals/`). They run the same on every operating system with Docker, like the Testcontainers tests |
| AC-9 | Manual validation with the real Codex CLI in its image, recorded in the PR (EV-1) |
| AC-10 | Fake adapter tests |
| AC-11 | A log-capture test asserting no key or token appears |
| AC-12 | API test |
| EV-3 | Optional manual check with `openai` and `anthropic` keys, recorded in the PR when run |

## Risks and mitigations

- CLI flags change between versions → presets are data, not code; the preset declares
  the tool version it was validated with; the startup log warns when that version
  differs.
- A CLI tool hangs when started headless (reported for `agy`) → mandatory timeout that
  stops the container; stdin is closed after the prompt is written.
- A CLI tool writes outside its sandbox → the workspace is mounted read-only and nothing
  else of the host is mounted (ADR-0030); presets also keep the tool's own read-only
  mode.
- A tool keeps sessions or history next to its login, so state could carry over between
  calls → the preset uses the tool's ephemeral mode (for Codex, its documented
  ephemeral option) or redirects that state to `/scratch`; EV-1 checks that the credential
  volume does not grow with session files.
- A tool's login does not work in a container (for example, it needs a local browser) →
  prefer the tool's device-code or token login; if a tool has none, its preset stays "not
  yet validated" and the README says so.
- Docker Desktop or Rancher Desktop is not running on the maintainer's machine → a clear
  error at call time, and the README lists Docker as a requirement of the CLI mode.
