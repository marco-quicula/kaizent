package br.com.refatorado.kaizent.run;

import java.util.UUID;

/** Starts evaluation runs (spec 0003): runs with {@link ReviewMode#SKIP}. */
public interface EvaluationRuns {

  /**
   * Creates a {@code RECEIVED} run with review mode {@link ReviewMode#SKIP} for a stored problem.
   * The worker resolves it, and it ends as {@link RunStatus#EVALUATED} or {@link RunStatus#FAILED}.
   */
  Run start(UUID problemId);
}
