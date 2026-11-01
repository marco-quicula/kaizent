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
  CliResult execute(CliCommand command);   // command, args, stdin or prompt file, cwd, env allowlist, timeout
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

**Preset shape** (YAML). The exact flags are verified with the installed tool's `--help`
during implementation:

```yaml
name: codex
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

**`LocalProcessExecutor`:** `ProcessBuilder`, a clean environment plus an allowlist
(`PATH`, `HOME`, and what the preset declares), destruction of the process tree on
timeout (`ProcessHandle.descendants()`), and capped output buffers.

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
```

## Testing

| AC | Test |
|---|---|
| AC-1 | Context tests with valid and invalid profiles |
| AC-2 | Unit tests with a stubbed `ChatModel`: valid output, invalid then repaired, invalid twice |
| AC-3 | Conditional activation tests |
| AC-4 | Retry tests with a stub that fails with 429 and then succeeds |
| AC-5 | Pricing computation tests |
| AC-6 to AC-8 | `LocalProcessExecutor` and CLI adapter tests against `src/test/resources/fake-cli/*.sh` (prints canned JSONL, sleeps for timeouts, exits non-zero) |
| AC-9 | Manual validation with the real Codex CLI, recorded in the PR (EV-1) |
| AC-10 | Fake adapter tests |
| AC-11 | A log-capture test asserting no key or token appears |
| AC-12 | API test |

## Risks and mitigations

- CLI flags change between versions → presets are data, not code; the preset declares
  the tool version it was validated with; the startup log warns when that version
  differs.
- A CLI tool hangs when started as a subprocess (reported for `agy`) → mandatory timeout
  and process-tree kill; stdin is closed after the prompt is written.
- A CLI tool writes outside its sandbox → presets always use the tool's read-only mode,
  and Kaizent passes only the workspace as the working directory. The README states that
  the CLI sandbox is the tool's responsibility.
