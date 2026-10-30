# ADR-0001: Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

Kaizent is a public project that will evolve over several waves and, we hope, with
several contributors. Decisions about license, stack and architecture need to be
discoverable, together with the reasons behind them, so that newcomers and future
sessions do not reopen settled questions without new information.

## Decision

We record every significant decision as an Architecture Decision Record (ADR) in
`docs/adr/`, using this lightweight format:

- file name `NNNN-short-title.md`, numbered sequentially;
- sections: Status, Date, Context, Decision, Alternatives considered, Consequences;
- status is one of `Proposed`, `Accepted`, `Superseded by ADR-NNNN` or `Deprecated`.

An accepted ADR is not edited to change its decision. It is superseded by a new ADR.
Decisions that are still pending are tracked in the "Open decisions" list in
[README.md](README.md).

## Alternatives considered

- **Decisions in a wiki or in issues.** These are harder to version with the code and
  easy to lose.
- **No formal records.** Too fragile for an open-source project with an incremental
  roadmap.

## Consequences

- Every new decision of architectural weight comes with an ADR in the same pull request.
- The ADR index and the open decisions list must be kept up to date.
