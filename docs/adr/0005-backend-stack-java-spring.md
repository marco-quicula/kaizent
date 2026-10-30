# ADR-0005: Backend stack: Java 25, Maven, Spring Boot and Spring AI

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

The maintainer has deep Java experience, some .NET experience, and runs Kubernetes on
premise. They already integrate OpenAI, Anthropic and Gemini through their native SDKs,
and MCP as well. Python dominates the AI tooling ecosystem, but the JVM now has mature
options. Using an existing framework is acceptable, and learning it is part of the goal.

## Decision

- **Language:** Java 25 (LTS).
- **Build:** Maven, with the Maven Wrapper (`mvnw`) committed so contributors need no
  local installation.
- **Framework:** Spring Boot 4.x.
- **AI layer:** Spring AI (the latest GA release compatible with the chosen Spring Boot
  version). It provides the ChatClient, tool calling, structured output, advisors, MCP
  client support, vector store abstractions and providers for Anthropic, OpenAI, Gemini
  and Ollama.
- **Testing:** JUnit 5 and Testcontainers.
- Exact versions are pinned when implementation starts.

## Alternatives considered

| Option | Why not (for now) |
|---|---|
| Python (LangGraph, LlamaIndex, etc.) | It has the richest AI ecosystem, but it is the maintainer's weaker stack and is less aligned with Spring-based enterprise environments. |
| TypeScript (Vercel AI SDK, Mastra) | Good agent tooling, but it would be a second primary language. TypeScript is already used for the frontend only (ADR-0008). |
| .NET (Semantic Kernel) | Viable, but the maintainer has less experience with it. |
| LangChain4j | A solid JVM alternative, but Spring AI integrates more naturally with Spring Boot. It stays an option behind our ports. |
| Embabel (agent framework on the JVM, built on Spring AI) | Promising for goal-oriented agent planning. We will evaluate it when we design the agent loop (see the open decisions). |
| Native provider SDKs only | Maximum control, but every provider would need its own glue code. They remain an escape hatch behind the `LlmGateway` port (ADR-0007). |

## Consequences

- Contributors need JDK 25. Docker is needed for the local database and for the tests.
- Spring AI sometimes lags behind provider-specific features. When that happens, an
  adapter using the native SDK can be added behind the same port.
- The Spring ecosystem gives us a direct path to observability (Micrometer and
  OpenTelemetry) and to Kubernetes deployment in later waves.
