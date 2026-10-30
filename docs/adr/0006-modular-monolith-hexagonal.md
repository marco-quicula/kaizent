# ADR-0006: Modular monolith with a hexagonal (ports and adapters) architecture

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

Kaizent must plug into many technologies that are chosen at setup time:

- ticket sources (files, Movidesk, Slack, chat);
- reply sinks;
- skill repositories;
- knowledge sources (Markdown, vector stores, MCP);
- tools (MCP);
- LLM providers.

The core pipeline (triage → role → context → resolve → review → learn) must not depend
on any of them. The project is maintained by one person at first, and deployment must
stay simple for local runs and for on-premise Kubernetes.

## Decision

- A **modular monolith**: a single deployable Spring Boot application, split into
  modules whose boundaries are enforced with **Spring Modulith** (verification tests and
  documentation).
- A **hexagonal architecture** inside it. The domain and the application services
  depend only on **ports** (interfaces). Every technology lives in an **adapter**.
- The initial ports:

| Port | Direction | MVP adapter |
|---|---|---|
| `TicketSource` | Inbound | File folder in a git repository |
| `ReplySink` | Outbound | File folder |
| `SkillRepository` | Outbound | Git folder of `SKILL.md` skills |
| `KnowledgeSource` | Outbound | Markdown files in git |
| `ToolProvider` | Outbound | Built-in tools (MCP in wave 2) |
| `LlmGateway` | Outbound | Spring AI |
| `RunStore` | Outbound | PostgreSQL |

- Adapters are selected and configured at setup time (Spring configuration properties),
  so a deployment can, for example, use Movidesk + git skills + pgvector.

## Alternatives considered

- **Microservices.** Operational cost without benefit at this stage. Module boundaries
  keep the option open to extract services later.
- **A layered architecture without ports.** Simpler at first, but channels and
  technologies would leak into the core, which defeats the plug-in goal.

## Consequences

- New channels and technologies are added as adapters without changing the core.
- Ports must be designed around the domain's needs, not around the first adapter's API.
- The Maven module layout (a single module with Modulith packages, or several Maven
  modules) is still open (see the open decisions).
