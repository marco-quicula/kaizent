package br.com.refatorado.kaizent.run;

/**
 * Whether a run waits for a human verdict (spec 0003). {@link #SKIP} is the persistent marker of an
 * evaluation run, whatever its status: consumers that must leave evaluation runs out filter on the
 * review mode, never on the status.
 */
public enum ReviewMode {
  /** The default: the proposal waits for a reviewer's verdict and the reply is delivered. */
  HUMAN,
  /**
   * Evaluation only: the run ends in {@link RunStatus#EVALUATED} after resolution and is never
   * reviewed or delivered.
   */
  SKIP
}
