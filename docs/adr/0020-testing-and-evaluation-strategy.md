# ADR-0020: Testing and evaluation strategy

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

Kaizent is a public repository: CI cannot hold LLM credentials, and real model calls cost
money and are not deterministic. We still need confidence that the pipeline works, and a
way to judge the quality of LLM-dependent behavior.

## Decision

- **Tests (CI, deterministic, no network to LLMs):**
  - unit tests for the domain and application services;
  - a Spring Modulith verification test for the module boundaries;
  - integration tests with Testcontainers PostgreSQL;
  - **fake `LlmGateway` and `AgentRuntime` adapters** that return scripted responses;
  - CLI adapter tests against a **fake CLI script** that prints canned JSON, which
    covers process handling, timeouts and output parsing;
  - API tests against the OpenAPI contract;
  - console tests with Vitest and React Testing Library.
- **Evaluations (opt-in, real LLM, never in CI):**
  - evaluation cases live in the workspace (`evals/cases/*.yaml`). Each case points to a
    ticket and lists expected properties: subject, problem type, sources that must be
    cited, facts that must be mentioned, statements that must not appear, and whether
    low confidence is expected;
  - an evaluation runner (a Spring profile of the application) runs the cases against the
    configured adapters and writes a Markdown report with pass/fail per property, tokens
    and cost;
  - evaluations are run manually by whoever has LLM access, for example before merging a
    change to prompts, skills or adapters.
- Acceptance criteria for LLM-dependent behavior are written as evaluation cases
  (ADR-0015).

## Alternatives considered

- **Real LLM calls in CI with a repository secret.** Not possible for pull requests from
  forks, costly and flaky.
- **Recorded HTTP interactions (VCR style).** Brittle across providers and CLI tools.
  Scripted fakes at the port level are simpler.

## Consequences

- CI proves that the pipeline is correct, not that the answers are good. Evaluations
  measure answer quality.
- The evaluation set grows with the demo workspace and, from wave 4, with recorded runs.
