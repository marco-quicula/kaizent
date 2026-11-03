package br.com.refatorado.kaizent.run;

/**
 * The run states of ADR-0023. Spec 0002 uses {@code RECEIVED → RESOLVING → AWAITING_REVIEW →
 * APPROVED → DELIVERED} and {@code FAILED}; spec 0003 adds {@code EVALUATED}; triage ({@code
 * TRIAGING}) arrives in spec 0006.
 */
public enum RunStatus {
  RECEIVED,
  TRIAGING,
  RESOLVING,
  AWAITING_REVIEW,
  APPROVED,
  DELIVERED,
  REJECTED,
  FAILED,
  /** Terminal state of an evaluation run ({@link ReviewMode#SKIP}) that produced a proposal. */
  EVALUATED
}
