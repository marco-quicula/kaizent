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
| `resolution` | `RuntimeSelector` | Chooses the runtime for a run (AC-11): the configured one for a `SAFE` ticket; for a `SUSPICIOUS` one, the native runtime with `kaizent.security.api-runtime` (`api-runtime`) or the configured one (`flag-only`), never a CLI preset with `read-restricted: false`; fails the run with a clear message when no allowed runtime is configured |
| `resolution` | `OutputLeakCheck` | Redacts token and key formats, paths outside the granted ones and URLs to unknown hosts in the diagnosis and the reply, and returns one flag per redaction (AC-12) |
| `resolution` | `SecurityPolicyWarning` | Logs a WARN at startup when `kaizent.security.suspicious-tickets` is not `api-runtime` (AC-11) |

Runtime selection: `kaizent.agent.resolution.runtime = native | cli | fake`. With
`native`, `kaizent.agent.resolution.adapter` and `model` choose the Spring AI provider.

## Output record

```java
record ProposalOutput(String diagnosis, String suggestedReply, double rawConfidence,
                      String confidenceRationale, List<Citation> citations,
                      List<String> skillsUsed, boolean lacksInformation) {}
record Citation(String path, @Nullable String section) {}
```

The stored `Proposal` adds `adjustedConfidence`, `appliedRules`, `invalidCitations`, the
skills with their versions, `resolvedBy` (the runtime and its adapter or preset) and
`securityFlags` (one per redaction of the output check, with the kind and the field; the
redacted text itself is not stored).

## Prompt resource

`prompts/resolution.md` covers:

- the role persona;
- an instruction to use skills by reading them;
- citation rules (relative paths, only documents actually read);
- honesty rules (set `lacksInformation` instead of guessing; never invent features);
- the reply language.

## Data

None new. The proposal is stored as JSONB on `run`, with the shape of spec 0002 extended
(including `resolvedBy` and `securityFlags`). The injection risk comes from the triage
document of spec 0006. `RunDetail` shows `resolvedBy` and the security flags (contract
first).

## Configuration

```yaml
kaizent:
  agent.resolution: { runtime: native, adapter: gemini-api, model: <id> }  # or runtime: cli, preset: codex
  budget.max-tool-calls: 20
  confidence: { no-citation-cap: 0.5, invalid-citation-cap: 0.3, lacks-information-cap: 0.3 }
  security:
    suspicious-tickets: api-runtime        # or flag-only (operator's risk; WARN at startup)
    api-runtime: { adapter: gemini-api, model: <id> }   # the native runtime for suspicious tickets
    known-hosts: []                        # hosts whose URLs are not notes (spec 0006) nor redacted
  cli.presets.<name>.agent.read-restricted: true   # validated per tool (spec 0004 T8)
```

## Testing

| AC | Test |
|---|---|
| AC-1 | `TaskAssembler` unit test |
| AC-2, AC-3 | `NativeAgentRuntime` tests with a stub `ChatModel` that scripts tool calls (in scope, out of scope, over the limit) |
| AC-4 | CLI runtime test with a fake CLI script, asserting the paths passed |
| AC-5 to AC-8 | `ProposalValidator` and `ConfidencePolicy` unit tests |
| AC-9, AC-10 | Orchestrator and end-to-end tests with the fake runtime |
| AC-11 | `RuntimeSelector` tests for every combination of risk, policy and runtime: a suspicious ticket with a CLI runtime goes to the API runtime; without one, the run fails with the message; `flag-only` keeps the configured runtime but never a preset with `read-restricted: false`; a log-capture test of the startup WARN; an orchestrator test that records `resolvedBy` |
| AC-12 | `OutputLeakCheck` unit tests with synthetic tokens and keys, outside paths and unknown URLs (redacted and flagged) and with legitimate text (unchanged); an end-to-end test with a fake runtime whose answer leaks a synthetic token |
| EV-1 to EV-6 | Evaluation runner with two profiles; results recorded in the PR |

## Risks and mitigations

- The model cites documents it never read → the native runtime can cross-check
  citations against the `read_document` calls; flag them as "unread citation" (reported,
  without a confidence cap in wave 1).
- Native and CLI runtimes behave differently → the evaluations run with both, and the
  differences are reported in the PR.
- Redaction hides a legitimate part of the reply (for example a documented URL) → known
  hosts are configurable, every redaction is flagged for the reviewer, and the human
  verdict stays mandatory.
