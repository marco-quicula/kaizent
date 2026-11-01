# ADR-0022: Confidence scoring

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

Each proposal carries a confidence level. It is shown to the reviewer now and will drive
autonomy in wave 4. A model's self-reported confidence is not calibrated, but there is no
data yet to calibrate it.

## Decision

- The agent reports a **raw confidence** between 0 and 1, with a short rationale, as part
  of the structured proposal.
- Deterministic **evidence rules** produce an **adjusted confidence**:
  - no cited source → capped at **0.5**;
  - any cited path that does not exist in the workspace → capped at **0.3**, and the
    proposal is flagged "invalid citation";
  - the agent declares that it lacks information → capped at **0.3**.
- The console shows the adjusted value as a band: **low** (< 0.5), **medium** (0.5 to
  0.8) and **high** (≥ 0.8), with the raw value and the rules applied.
- Both values are stored with every run so they can be **calibrated against verdicts**
  later (wave 4).

## Alternatives considered

- **Self-reported confidence only.** It is easily overconfident.
- **Confidence from token log-probabilities.** Not available consistently across
  providers, and not available at all through CLI tools.

## Consequences

- The rules are simple and explainable, and are expected to be revised once data exists.
- The cap values are configuration, not code constants.
