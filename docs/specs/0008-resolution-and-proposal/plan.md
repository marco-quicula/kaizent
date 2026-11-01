# Plan 0008: Resolution and proposal

## Components

| Module | Component | Responsibility |
|---|---|---|
| `resolution` | `TaskAssembler` | Builds the `AgentTask` from the run, the role, `SkillRepository.catalog` and the prompt template |
| `resolution` | `NativeAgentRuntime` (`adapter.native`) | Spring AI `ChatClient` with `@Tool` methods bound to a per-run `ToolScope`; counts tool calls; emits `TraceEvent`s to the `TraceSink`; requests structured output for the final answer |
| `resolution` | `ToolScope` | Holds the role's knowledge roots and allowed skills; records the skills read |
| `resolution` | `ProposalValidator` | Schema check, citation checks (through `WorkspacePaths`), filtering of the skills used |
| `resolution` | `ConfidencePolicy` | Applies the configurable caps of ADR-0022 |
| `resolution` | `ResolutionService` | Orchestrates the steps: assemble, run, validate, score, return a `Proposal` |
| `run` | `RunOrchestrator` | Calls `ResolutionService`; handles `BUDGET_EXCEEDED` |

Runtime selection: `kaizent.agent.resolution.runtime = native | cli | fake`. With
`native`, `kaizent.agent.resolution.adapter` and `model` choose the Spring AI provider.

## Output record

```java
record ProposalOutput(String diagnosis, String suggestedReply, double rawConfidence,
                      String confidenceRationale, List<Citation> citations,
                      List<String> skillsUsed, boolean lacksInformation) {}
record Citation(String path, @Nullable String section) {}
```

The stored `Proposal` adds `adjustedConfidence`, `appliedRules`, `invalidCitations` and
the skills with their versions.

## Prompt resource

`prompts/resolution.md` covers:

- the role persona;
- an instruction to use skills by reading them;
- citation rules (relative paths, only documents actually read);
- honesty rules (set `lacksInformation` instead of guessing; never invent features);
- the reply language.

## Data

None new. The proposal is stored as JSONB on `run`, with the shape of spec 0002 extended.

## Configuration

```yaml
kaizent:
  agent.resolution: { runtime: native, adapter: gemini-api, model: <id> }  # or runtime: cli, preset: codex
  budget.max-tool-calls: 20
  confidence: { no-citation-cap: 0.5, invalid-citation-cap: 0.3, lacks-information-cap: 0.3 }
```

## Testing

| AC | Test |
|---|---|
| AC-1 | `TaskAssembler` unit test |
| AC-2, AC-3 | `NativeAgentRuntime` tests with a stub `ChatModel` that scripts tool calls (in scope, out of scope, over the limit) |
| AC-4 | CLI runtime test with a fake CLI script, asserting the paths passed |
| AC-5 to AC-8 | `ProposalValidator` and `ConfidencePolicy` unit tests |
| AC-9, AC-10 | Orchestrator and end-to-end tests with the fake runtime |
| EV-1 to EV-6 | Evaluation runner with two profiles; results recorded in the PR |

## Risks and mitigations

- The model cites documents it never read → the native runtime can cross-check
  citations against the `read_document` calls; flag them as "unread citation" (reported,
  without a confidence cap in wave 1).
- Native and CLI runtimes behave differently → the evaluations run with both, and the
  differences are reported in the PR.
