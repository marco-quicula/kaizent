package br.com.refatorado.kaizent.run;

import java.util.UUID;

/** Applies a reviewer's verdict to a run and delivers the reply (ADR-0023). */
public interface RunReviews {

  /**
   * Records the verdict on a run that is {@code AWAITING_REVIEW}. An {@code APPROVED} verdict moves
   * the run to {@code APPROVED}, delivers the final reply and moves it to {@code DELIVERED}; if the
   * delivery fails, the run becomes {@code FAILED} with reason {@code ERROR}.
   *
   * @return the run in its final state
   * @throws RunNotFoundException if the run does not exist
   * @throws RunStateException if the run is not awaiting review
   */
  Run recordVerdict(UUID runId, Verdict verdict);
}
