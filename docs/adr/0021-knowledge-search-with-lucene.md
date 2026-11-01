# ADR-0021: Knowledge search with an in-memory Lucene index

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

ADR-0011 put the MVP's knowledge in Markdown files in git, accessed through
`search_knowledge` and `read_document`. `search_knowledge` needs ranked text search. The
options were an in-memory index or PostgreSQL full-text search.

## Decision

- `search_knowledge` uses **Apache Lucene** with an **in-memory index** built from the
  workspace's `knowledge/` folder.
- The index is built at startup and updated when files change (file watching), and also
  right after Kaizent writes an approved lesson.
- Documents are indexed per Markdown section (heading-based chunks) with their file path,
  title and front matter tags, so results can point to a section.
- Results return the relative path, the section heading, a snippet and a score.

## Alternatives considered

- **PostgreSQL full-text search.** It would require syncing files into the database, and
  the source of truth would then exist in two places.
- **A plain substring scan.** No ranking, and poor results as the knowledge base grows.

## Consequences

- No extra infrastructure. Apache Lucene is Apache-2.0 licensed.
- The index is rebuilt on every start, which is fine for a small knowledge base.
- The vector store (wave 3) is added as another knowledge adapter. Hybrid search can then
  combine both.
