# ADR-0012: Human in the loop, with autonomy levels earned per subject

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

Agents that answer customers directly need to be trusted, and trust has to be measured.
Help desk channels do not always provide outcome signals such as ratings. Human review
gives us both a safety net and a success signal.

## Decision

- In the MVP, **every proposal requires a human verdict** before anything leaves the
  system.
- The verdicts are **approved as is**, **approved with edits** and **rejected with a
  comment**. The difference between the proposal and the final answer is stored.
- Metrics are tracked **per subject**. The primary metric is the share of proposals
  approved without edits. Later waves add outcome signals (no reopen, ratings) when the
  channel provides them.
- Each subject has an **autonomy level** derived from its metrics: **Junior → Mid-level
  → Senior → Specialist**. Each level will define how much review is required (for
  example, every answer, a sample, or only answers below a confidence threshold).
- In the MVP the level is **computed and displayed but does not change behavior**.
  Autonomy starts to act in wave 4.

## Alternatives considered

- **Automatic replies above a confidence threshold from day one.** Self-reported
  confidence is not calibrated without data, so this is unsafe.
- **A global autonomy level.** Too coarse: the agent can be excellent at one subject and
  poor at another.

## Consequences

- Reviewer effort is high at the start. That cost is intended, because it produces the
  labeled data the later waves need.
- The thresholds and the minimum sample size for each level are still open (see the open
  decisions).
- A human can always demote a subject's level or force review.
