# ADR-0028: Ticket file format and canonical problem model

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

ADR-0014 introduced the canonical `Problem` model and file-based tickets for the MVP. The
file format is the first concrete mapping to the canonical model, and an example for
future connectors.

## Decision

- A ticket file is Markdown with optional YAML front matter:

```markdown
---
id: PC-0001                 # optional; defaults to the file name without extension
title: Error when adding a pet with a future birth date   # optional; defaults to the first line
requester: Jane Doe         # optional; synthetic in the demo
channel: email              # optional; informative only
created: 2026-10-01T10:00:00Z   # optional; defaults to the file's modification time
labels: [pets]              # optional; hints only, never trusted as the subject
---
Free-form description. Stack traces go in fenced code blocks.
```

- The canonical **`Problem`** has these fields: `id`, `source` (connector id, for example
  `file-inbox`), `externalKey` (ticket id), `title`, `body`, `requester`, `channel`,
  `createdAt`, `receivedAt`, `labels`, `contentHash` and `attributes` (a free map for
  connector-specific data).
- **Deduplication:** a ticket is new if its `(source, externalKey)` has not been seen. If
  the same key arrives with a different `contentHash`, the change is recorded as an
  **intake warning** and no new run is created (updates to tickets are out of scope for
  wave 1).
- Invalid files (unreadable, invalid front matter, empty body) become **intake errors**,
  visible in the console. They never create runs.
- An approved reply is written to `outbox/<ticket-id>.reply.md`, with front matter
  (`ticket`, `run`, `verdict`, `approvedAt`, `language`) followed by the reply text.

## Alternatives considered

- **JSON or YAML-only ticket files.** Less pleasant to write by hand, especially with
  stack traces.
- **Trusting labels as the subject.** Real channels carry noisy labels. Triage decides
  the subject.

## Consequences

- The format is easy to write by hand and easy to review in git.
- Future connectors map their payloads to the same `Problem` fields, with extras in
  `attributes`.
