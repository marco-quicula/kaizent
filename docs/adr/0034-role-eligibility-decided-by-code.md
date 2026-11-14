# ADR-0034: Role eligibility is decided by code, classification only chooses within it

- **Status:** Accepted
- **Date:** 2026-11-14

## Context

A role decides what a run can read: its persona, allowed skills and knowledge paths. The
triage step classifies the ticket, and the role follows from the classification. Two
questions came up in the external review (2026-11):

1. **Subject alone is too coarse.** A role per subject cannot tell a bug from a how-to
   question on the same subject.
2. **Who may widen access?** If the model, or a rule over the ticket's labels, could pick any
   role from a list, then text controlled by the requester would decide what the agent can
   read. A closed list validates an identifier; it does not authorize the ticket to use that
   role. Labels are unverified hints (ADR-0028).

## Decision

- **Code decides which roles are eligible** for a ticket, by the ticket's origin and the
  workspace's policy. In wave 1 every role of the workspace is eligible for every ticket (the
  only source is the file inbox of a local operator), but the eligible set is computed by code
  and the rest of the selection works inside it.
- **The role comes from a deterministic mapping** by **subject and problem type**: the roles
  declare the subjects and, optionally, the problem types they serve (spec 0006). Precedence
  and the no-match case are defined there.
- **The triage model only classifies**: subject, problem type, language and a rationale. It
  never names a role and never widens access.
- **A ticket's labels are untrusted hints** for the classification (ADR-0028). They are never
  an input of the mapping or of eligibility.
- **Triage modes are wave 2.** The modes `llm`, `rules` and `rules-then-llm`
  (`kaizent.triage.mode`), where a rule or the model chooses among eligible candidates, are
  recorded for wave 2, when a real connector provides trusted metadata (for example the
  channel, the customer's plan or the queue) and its provenance. Wave 1 has one behavior:
  the LLM classifies, the code maps.

## Alternatives considered

- **The model picks the role from the list.** Simple, but a ticket's text then steers access.
- **Rules over labels.** Labels are written by the requester in the file inbox; the rules
  would widen access with data the requester controls.
- **A role per subject only (the previous rule).** Safe but coarse: it cannot route a
  stack-trace question and a usage question on the same subject to different roles.

## Consequences

- Spec 0006 changes: the role format gains the problem types, with a precedence rule and a
  default role of conservative scope for the no-match case.
- The run records the provenance of what decided the role (the classification, the matched
  mapping entry, or the default), so that a reviewer sees why.
- Wave 2 must define the trusted metadata and its provenance before it adds the modes.
