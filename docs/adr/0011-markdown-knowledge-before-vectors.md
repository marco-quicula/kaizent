# ADR-0011: Markdown knowledge in git before vector retrieval

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

The agent needs knowledge: documentation, FAQs, known issues and lessons from past
resolutions. A vector store is the usual answer, but it adds embeddings, indexing,
infrastructure and cost before we know what the knowledge looks like.

## Decision

- In the MVP, knowledge is **Markdown files in a git repository**, read through the
  `KnowledgeSource` port.
- The agent accesses it through tools: **`search_knowledge`** (text search returning
  ranked snippets with file references) and **`read_document`** (full file content).
- Approved lessons are written back as new Markdown files, so the knowledge base grows
  in a human-readable, reviewable form.
- Vector retrieval (pgvector, wave 3) and MCP knowledge servers (wave 2) are added later
  as additional adapters behind the same port.

## Alternatives considered

- **A vector store from day one.** Premature: it means more infrastructure and cost, and
  retrieval quality cannot be judged without real data.

## Consequences

- Retrieval quality in the MVP depends on text search and on the agent's ability to
  choose documents. That is acceptable for a small demo knowledge base.
- The text search implementation is still open (see the open decisions).
- The Markdown corpus becomes the ingestion source for the vector store in wave 3.
