# ADR-0004: English for all repository content

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

The maintainer is a Brazilian Portuguese speaker, and the project is public and aims at
an international community of users and contributors.

## Decision

- Everything committed to the repository is written in **English (en-US)**:
  documentation, ADRs, code, identifiers, comments, commit messages, issue and PR
  templates, log messages and UI text.
- Conversations (planning sessions, discussions with the maintainer) may happen in
  Portuguese. Their outcomes are recorded in English.
- Demo data (tickets, knowledge base) is in English.

## Alternatives considered

- **Portuguese, or mixed languages.** This would exclude most potential contributors and
  create inconsistent documentation.

## Consequences

- Contributors and AI assistants must translate decisions into English before committing.
- Internationalization of the UI or of agent replies (for example, answering tickets in
  Portuguese) is a product feature to design later. It does not change this policy.
