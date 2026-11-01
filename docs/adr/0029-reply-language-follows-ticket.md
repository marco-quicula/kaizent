# ADR-0029: Replies use the ticket's language

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

All repository content, including the demo data, is in English (ADR-0004). Real tickets
arrive in the customer's language, for example Brazilian Portuguese.

## Decision

- Triage detects the ticket's language (an ISO 639-1 code) as part of its structured
  output.
- The suggested reply is written **in the ticket's language**. The diagnosis, the
  rationale and other internal fields are written in English, so the trace stays
  consistent for reviewers and contributors.
- Prompts, skills and knowledge stay in English. The model translates when it writes the
  reply.

## Alternatives considered

- **Always reply in English.** Unusable for real customers.
- **Prompts and knowledge per language.** Duplicated content, which we avoid until it
  proves necessary.

## Consequences

- The demo includes at least one ticket in Brazilian Portuguese, with an evaluation case
  that checks the reply language.
- Reply quality in other languages depends on the model, and evaluations should cover
  it.
