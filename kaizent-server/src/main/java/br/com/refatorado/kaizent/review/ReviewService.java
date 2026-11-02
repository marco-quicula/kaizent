package br.com.refatorado.kaizent.review;

import br.com.refatorado.kaizent.run.Run;
import br.com.refatorado.kaizent.run.RunNotFoundException;
import br.com.refatorado.kaizent.run.RunReviews;
import br.com.refatorado.kaizent.run.RunStateException;
import br.com.refatorado.kaizent.run.RunStatus;
import br.com.refatorado.kaizent.run.Runs;
import br.com.refatorado.kaizent.run.Verdict;
import br.com.refatorado.kaizent.run.VerdictType;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Records verdicts. Spec 0002 accepts {@code APPROVED} only: the suggested reply becomes the final
 * reply. Spec 0010 adds edits and rejections.
 */
@Service
public class ReviewService {

  /** Wave 1 has no authentication (ADR-0012), so every verdict comes from the local reviewer. */
  static final String LOCAL_REVIEWER = "local";

  private final Runs runs;
  private final RunReviews reviews;
  private final Clock clock;

  ReviewService(Runs runs, RunReviews reviews, Clock clock) {
    this.runs = runs;
    this.reviews = reviews;
    this.clock = clock;
  }

  /**
   * Records the verdict and returns the run in its final state.
   *
   * @throws UnsupportedVerdictException if the type is not {@code APPROVED}
   * @throws RunNotFoundException if the run does not exist
   * @throws RunStateException if the run is not awaiting review
   */
  public Run recordVerdict(UUID runId, VerdictType type) {
    if (type != VerdictType.APPROVED) {
      throw new UnsupportedVerdictException(type);
    }
    Run run = runs.findById(runId).orElseThrow(() -> new RunNotFoundException(runId));
    if (run.status() != RunStatus.AWAITING_REVIEW || run.proposal() == null) {
      throw new RunStateException(run.id(), run.status(), RunStatus.AWAITING_REVIEW);
    }
    Verdict verdict =
        new Verdict(
            VerdictType.APPROVED,
            run.proposal().suggestedReply(),
            null,
            LOCAL_REVIEWER,
            clock.instant());
    return reviews.recordVerdict(runId, verdict);
  }
}
