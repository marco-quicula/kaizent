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
| `gemini-api`, `openai`, `anthropic`, `ollama` | `LlmGateway` | A Spring AI `ChatModel` per model id, built on the first call (so the application starts without keys). The answer goes through `StructuredOutputs` (schema in the prompt, validation, one repair). Spring AI's and the SDKs' own retries are off; `Retrying` retries instead, and `ProviderErrors` turns each SDK's exception into an `LlmCallException` with the status, the provider's code and message, and the suggested wait. The configured key is removed from the message and the causes, since a provider may echo it. For Gemini, the SDK's own retries are set to one attempt, and an OkHttp interceptor keeps the JSON `error.status` that the SDK drops |
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
  temporary paths that its tool needs), `--user` non-root (`kaizent.cli.user`, a numeric
  `uid:gid` with neither id zero, default `10001:10001`, the user that every CLI image
  creates; user names such as `root` are refused), `--cap-drop ALL`,
  `--security-opt no-new-privileges`, and `--cpus`, `--memory` and `--pids-limit` from
  `kaizent.cli.limits`;
- only the paths that the `CliCommand` grants, each mounted read-only at its relative
  path (`-v <workspace>/knowledge/product:/workspace/knowledge/product:ro`, and so on),
  with `-w /workspace`, so that citations keep their workspace-relative paths and nothing
  else of the workspace is visible; a per-call scratch folder mounted writable at
  `/scratch` for the prompt, the schema file, output files and the tool's other state;
  and the preset's credential volume at the preset's path;
- only the preset's `env` variables (`--env NAME`, with values from the environment or
  `.env`), never the whole host environment. The values reach `docker` through its own
  process environment, never on its command line, and an error excerpt replaces them with
  `[REDACTED]`: every occurrence of each value, and the start of a value that the output
  cap cut off at the end, is marked on the original text before anything is replaced or
  shortened, so that no value can hide or break another;
- the prompt written to stdin or to `/scratch`, as the preset defines, and stdin closed
  afterwards;
- capped output buffers (`kaizent.cli.max-output`, also for the output files read back
  from `/scratch`), and a clear error when `docker` is missing or Docker cannot start the
  container (exit code 125 with Docker's own message, for example when the daemon is not
  running).

When a call does not end normally (timeout, interruption or any other failure after the
container was started), the executor stops the container with `docker kill`: the container
belongs to the Docker daemon and keeps running when only its client is stopped. An
interruption is kept for the caller.

Failures are `CliException`s with a kind: `DOCKER_UNAVAILABLE`, `TIMEOUT`,
`NON_ZERO_EXIT` (with a truncated stderr excerpt) or `FAILED`. A granted path must be a
normalized workspace-relative path that stays inside the workspace after resolving
symbolic links, and it can neither be nor contain `evals/` nor resolve into it. An output
file that the tool left in `/scratch` is read only when it is a regular file: a symbolic
link fails the call, so that it cannot point to a host file that was never mounted. A CLI image creates the folder that its credential volume is mounted on,
owned by the tool's user, so that a new volume takes that ownership.

**Images and setup:** `docker/cli/<tool>/Dockerfile` installs the tool at a pinned
version on an official base image and adds a non-root user. `compose.yaml` gets a `cli`
profile with one service per tool, so that `docker compose --profile cli build` builds
the images and `docker compose run --rm <tool>-cli <login command>` fills the tool's
credential volume. The exact login command of each tool is verified during
implementation and documented in the README.

## Structured output

`StructuredOutputs` (in `llm.adapter`, shared by every adapter) appends the instructions
and the JSON schema generated from the output record (victools, Draft 2020-12, every
property required, no additional properties) to the user prompt. An answer is valid when
it is a JSON object (possibly inside a Markdown code fence) that validates against that
schema (networknt json-schema-validator), maps to the record with strict Jackson settings
(no type conversion), and meets the record's Bean Validation constraints, which can be
stricter than the schema (for example, a confidence from 0 to 1). On an invalid answer it
calls the model once more with the validation errors and the previous answer, and then
fails with `LlmOutputException`. The usage of both calls is added up. The pipeline's
output records (for example `ResolutionOutput`) are separate from the stored records
(`Proposal`), which also carry the usage.

**Usage per adapter:** the Gemini adapter reads the API's own usage metadata, not Spring
AI's normalized counts, which turn missing counts into zeros. Its billed output tokens
include the thinking tokens. A count that the API does not report stays unknown, so the
cost type becomes `UNKNOWN`. A configured price needs both rates, or startup fails.

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
    retry: { max-attempts: 3, initial-backoff: 2s, multiplier: 2, max-wait: 60s }
    openai: { api-key: ${OPENAI_API_KEY:}, base-url: ${OPENAI_BASE_URL:} }
    anthropic: { api-key: ${ANTHROPIC_API_KEY:} }
    ollama: { base-url: ${OLLAMA_BASE_URL:} }
    pricing:
      <model-id>: { input-per-million: 0.0, output-per-million: 0.0 }
  agent:
    resolution: { runtime: cli, preset: codex, timeout: 5m }
  cli:
    docker: docker            # the docker command
    max-output: 2MB
    user: "10001:10001"       # non-root; created by every CLI image
    limits: { cpus: 2, memory: 2g, pids: 256 }
```

## Testing

| AC | Test |
|---|---|
| AC-1 | Context tests with valid and invalid profiles |
| AC-2 | Unit tests with a stubbed `ChatModel`: valid output, invalid then repaired, invalid twice |
| AC-3 | Tests of each adapter against a local stub of its API (answer and token counts), and a startup test with an unconfigured adapter |
| AC-4 | Retry tests with a stub that fails with 429 and then succeeds, honors a suggested wait, and keeps the provider's cause when it gives up; error mapping tests for each SDK |
| AC-5 | Pricing computation tests |
| AC-6 to AC-8 | `DockerCliExecutor` and CLI adapter tests against a fake CLI image built from `src/test/resources/fake-cli/` (prints canned JSONL, sleeps for timeouts, exits non-zero, tries to write to the workspace, and tries to read paths it was not granted, including `evals/`). They run the same on every operating system with Docker, like the Testcontainers tests |
| AC-9 | Manual validation with the real Codex CLI in its image, recorded in the PR (EV-1) |
| AC-10 | Fake adapter tests |
| AC-11 | A log-capture test asserting no key or token appears |
| AC-12 | API test |
| EV-3 | Optional manual check with `openai` and `anthropic` keys, recorded in the PR when run |

**`llm-check` command (T13).** The manual validations run one triage-shaped structured call
on one ticket, before triage exists (spec 0006), with the `llm-check` profile:
`java -jar ... --spring.profiles.active=llm-check --kaizent.llm-check.ticket=PC-002`. It uses
the profile of `kaizent.llm-check.step` (default `resolution`), so any adapter, model or CLI
preset; prints the answer, the tokens, the cost and the cost type; stores and delivers
nothing; and exits with code 0, or 1 with the provider's cause when the call fails. It also
lets a user check a key, a model or a CLI login before running the pipeline. Like the
`eval` profile, it needs PostgreSQL running.

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
