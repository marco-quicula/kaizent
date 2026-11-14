# Plan 0008: Resolution and proposal

## Components

| Module | Component | Responsibility |
|---|---|---|
| `resolution` | `TaskAssembler` | Builds the `AgentTask` from the run, the role, `SkillRepository.catalog` and the prompt template |
| `resolution` | `NativeAgentRuntime` (`adapter.native`) | Spring AI `ChatClient` with `@Tool` methods bound to a per-run `ToolScope`; counts tool calls; checks the context size and the budget before every model turn (AC-3, spec 0009 `CallBudget`); emits `TraceEvent`s to the `TraceSink`; requests structured output for the final answer |
| `resolution` | `ToolScope` | Holds the role's knowledge roots and allowed skills; records the skills read; caps each tool result (`max-tool-result`), marks the truncation and adds the section outline (AC-2) |
| `resolution` | `ProposalValidator` | Schema check, citation checks (through `WorkspacePaths`), filtering of the skills used |
| `resolution` | `ConfidencePolicy` | Applies the configurable caps of ADR-0022 |
| `resolution` | `ResolutionService` | Orchestrates the steps: assemble, run, validate, score, return a `Proposal` |
| `run` | `RunOrchestrator` | Calls `ResolutionService`; handles `BUDGET_EXCEEDED` |
| `resolution` | `RuntimeSelector` | Picks the configured runtime (`native` or `fake`); at startup, refuses `runtime: cli` with a message that points to ADR-0033 (spec 0004 AC-8). The routing of suspicious tickets (ADR-0032 layer 3) comes back with the CLI runtime in wave 2 |
| `llm` (public API) | `OutputLeakCheck` | **The one barrier** of ADR-0032 layer 4, shared by `resolution`, `triage`, `learning` and the `TraceSink`. Redacts token and key formats, paths outside the granted ones (for a step with no granted path, such as triage and lesson, every absolute or home-folder path) and URLs to unknown hosts in every model-produced text that is stored or shown: diagnosis, reply, confidence rationale, citation paths and sections, the triage rationale, the lesson draft, and the model's messages, raw responses, repair attempts, errors, unknown events and tool-call arguments in trace events (before the `TraceSink` records them). Returns one flag per redaction with the field (AC-12) |

Runtime selection: `kaizent.agent.resolution.runtime = native | fake` (`cli` is wave 2, ADR-0033). With
`native`, `kaizent.agent.resolution.adapter` and `model` choose the Spring AI provider.

## Output record

```java
record ProposalOutput(String diagnosis, String suggestedReply, double rawConfidence,
                      String confidenceRationale, List<Citation> citations,
                      List<String> skillsUsed, boolean lacksInformation) {}
record Citation(String path, @Nullable String section) {}
// The stored citation adds `contentHash` and `version` of what the run actually read for it
// (absent when it was not read), so that the console can tell whether the document changed since.
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
  agent:
    resolution: { runtime: native, adapter: gemini-api, model: <id> }  # or runtime: fake; cli is wave 2 (ADR-0033)
    max-tool-result: 16KB                # cap on each tool result sent to the model (AC-2)
    max-context-tokens: <model limit>    # context check before each model turn (AC-3)
  budget: { max-tool-calls: 20, max-output-per-call: 4000 }   # the rest of the budget: spec 0009
  confidence: { no-citation-cap: 0.5, invalid-citation-cap: 0.3, lacks-information-cap: 0.3 }
  security:
    known-hosts: []                        # hosts whose URLs are not notes (spec 0006) nor redacted
  # wave 2 (ADR-0033): suspicious-tickets, api-runtime and cli.presets.<name>.agent.read-restricted
```

## Testing

| AC | Test |
|---|---|
| AC-1 | `TaskAssembler` unit test |
| AC-2, AC-3 | `NativeAgentRuntime` tests with a stub `ChatModel` that scripts tool calls (in scope, out of scope, over the limit); a result longer than `max-tool-result` is cut, marked and offers the outline, and another section can then be read; a context that would exceed the model limit fails with `BUDGET_EXCEEDED` before the call |
| AC-5 to AC-8 | `ProposalValidator` and `ConfidencePolicy` unit tests |
| AC-9, AC-10 | Orchestrator and end-to-end tests with the fake runtime |
| AC-11 | `RuntimeSelector` tests: `native` and `fake` start; `runtime: cli` fails at startup with the ADR-0033 message; an orchestrator test that a suspicious ticket is resolved by the native runtime, with its risk shown on the run, and that `resolvedBy` is recorded |
| AC-12 | `OutputLeakCheck` unit tests with synthetic tokens and keys, outside paths and unknown URLs (redacted and flagged) in each covered field, including `confidenceRationale`, a citation section, a trace message, the triage rationale and a lesson draft, and with legitimate text (unchanged); an end-to-end test with a fake runtime whose answer leaks a synthetic token in the reply and in the confidence rationale |
| EV-1 to EV-6 | Evaluation runner with the native-runtime API profile; results recorded in the PR |

## Risks and mitigations

- The model cites documents it never read → the native runtime can cross-check
  citations against the `read_document` calls; flag them as "unread citation" (reported,
  without a confidence cap in wave 1).
- Redaction hides a legitimate part of the reply (for example a documented URL) → known
  hosts are configurable, every redaction is flagged for the reviewer, and the human
  verdict stays mandatory.
