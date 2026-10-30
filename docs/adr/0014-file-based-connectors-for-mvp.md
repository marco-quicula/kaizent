# ADR-0014: File-based connectors and a canonical problem model for the MVP

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

The target channel is a help desk tool such as Movidesk, and others will follow (Slack,
chat). Integrating a real channel in the MVP would add credentials, webhooks and an
external account, and anyone wanting to run the project would need access to that tool.

## Decision

- The core works on a **canonical `Problem` model** that is independent of the channel.
  Every inbound connector maps its source format to it.
- The MVP ships **file-based connectors only**:
  - **inbound:** a folder (`inbox/`) in a git repository, with one ticket per file
    (Markdown body plus YAML front matter for metadata), polled periodically;
  - **outbound:** approved replies are written as files to an `outbox/` folder.
- Movidesk is the first real connector, in wave 2.

## Alternatives considered

- **Movidesk in the MVP.** Realistic, but it blocks contributors without an account and
  adds secrets to manage.
- **A REST endpoint for ingestion.** Useful later. Files are simpler to demo, version and
  test.

## Consequences

- The MVP can be run end to end with nothing but the repository and an LLM API key.
- The ticket file format becomes the reference example for the canonical model and has
  to be documented.
