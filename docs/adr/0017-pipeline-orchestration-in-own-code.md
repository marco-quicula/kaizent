# ADR-0017: Pipeline orchestration in our own code, on top of Spring AI

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

The resolution pipeline has fixed steps: intake → triage → role selection → context
assembly → resolution → review → delivery → learning. Only the resolution step is a free
agentic loop. We need full control over each step's trace, budget and state, because the
PDCA cycle, the metrics and the future evaluation all depend on it. The candidates are
plain Spring AI or an agent framework on the JVM such as Embabel (actively maintained,
version 1.5.x at the time of writing, built on Spring AI).

## Decision

- The **pipeline is orchestrated by our own code**: an explicit sequence of steps, each
  one persisted with its state and trace (ADR-0023).
- Within the resolution step, the API-based `AgentRuntime` uses **Spring AI's ChatClient
  with tool calling** directly (ADR-0016). CLI-based runtimes delegate the loop to the
  CLI tool.
- **Embabel is not adopted for now.** It stays on the radar for when planning becomes
  more complex, for example multi-step plans or several cooperating roles.

## Alternatives considered

- **Embabel.** Goal-oriented planning and a strong typed model, but it would own the
  control flow we need to trace and budget, and it adds a younger dependency to the MVP.
- **LangChain4j agents.** Same trade-off, and less aligned with Spring Boot than Spring
  AI.

## Consequences

- We write and maintain the orchestration code (small for a linear pipeline).
- Switching the resolution loop to a framework later only affects one `AgentRuntime`
  adapter.
