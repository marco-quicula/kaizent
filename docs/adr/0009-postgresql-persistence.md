# ADR-0009: PostgreSQL for operational data, with pgvector later

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

Every run must be stored with its full trace (input, context, tool calls, proposal,
verdict, final answer, tokens, cost) for review, metrics, evaluation and future
fine-tuning. Wave 3 adds a vector store.

## Decision

- **PostgreSQL** stores runs, verdicts, lessons and metrics. Schema migrations use
  **Flyway**.
- Locally, PostgreSQL runs through **`docker compose`**. Tests use **Testcontainers**.
- In wave 3 the vector store will start as **pgvector** in the same database, behind the
  `KnowledgeSource` port, so the stack stays at one database until there is a reason to
  add another.
- Skills and knowledge are **not** stored in the database. They live in git (ADR-0010,
  ADR-0011).

## Alternatives considered

- **Embedded H2 or files only.** Even fewer dependencies, but further from production
  and with no path to pgvector.
- **A dedicated vector database (Qdrant, Weaviate, Milvus).** Possible later through the
  port. Not justified at the start.

## Consequences

- Running locally requires Docker.
- A single database simplifies operations on premise.
