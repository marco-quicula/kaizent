package br.com.refatorado.kaizent.run;

/** Why a run is {@link RunStatus#FAILED} (ADR-0023). */
public enum FailureReason {
  ERROR,
  BUDGET_EXCEEDED,
  TIMEOUT
}
