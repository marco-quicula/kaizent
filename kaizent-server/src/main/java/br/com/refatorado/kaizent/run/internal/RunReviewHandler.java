package br.com.refatorado.kaizent.run.internal;

import br.com.refatorado.kaizent.delivery.Reply;
import br.com.refatorado.kaizent.delivery.ReplySink;
import br.com.refatorado.kaizent.intake.Problem;
import br.com.refatorado.kaizent.intake.Problems;
import br.com.refatorado.kaizent.run.FailureReason;
import br.com.refatorado.kaizent.run.Run;
import br.com.refatorado.kaizent.run.RunNotFoundException;
import br.com.refatorado.kaizent.run.RunReviews;
import br.com.refatorado.kaizent.run.Verdict;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code AWAITING_REVIEW → APPROVED → DELIVERED} (ADR-0023). The approval is committed before the
 * reply is written, so a failed delivery never loses the verdict: the run becomes {@code FAILED}
 * with the delivery error instead. Concurrent verdicts are rejected by the run's optimistic lock.
 */
@Component
class RunReviewHandler implements RunReviews {

  /** Wave 1 detects no language yet; spec 0006 adds it. BCP 47 "undetermined". */
  static final String UNDETERMINED_LANGUAGE = "und";

  private static final Logger log = LoggerFactory.getLogger(RunReviewHandler.class);

  private final RunRepository runs;
  private final Problems problems;
  private final ReplySink replies;
  private final TransactionTemplate transactions;
  private final Clock clock;

  RunReviewHandler(
      RunRepository runs,
      Problems problems,
      ReplySink replies,
      TransactionTemplate transactions,
      Clock clock) {
    this.runs = runs;
    this.problems = problems;
    this.replies = replies;
    this.transactions = transactions;
    this.clock = clock;
  }

  @Override
  public Run recordVerdict(UUID runId, Verdict verdict) {
    Run approved =
        transactions.execute(status -> {
          Run run = runs.findById(runId).orElseThrow(() -> new RunNotFoundException(runId));
          return runs.save(run.approve(verdict, clock.instant()));
        });
    Run result;
    try {
      Problem problem =
          problems
              .findById(approved.problemId())
              .orElseThrow(() -> new IllegalStateException("Problem not found"));
      replies.send(
          new Reply(
              problem.externalKey(),
              approved.id(),
              verdict.type().name(),
              verdict.reviewedAt(),
              UNDETERMINED_LANGUAGE,
              verdict.finalReply()));
      result = approved.delivered(clock.instant());
      log.info("Run {} for {} delivered", approved.id(), problem.externalKey());
    } catch (RuntimeException e) {
      log.warn("Delivery of run {} failed: {}", approved.id(), e.getMessage());
      result =
          approved.fail(FailureReason.ERROR, "Delivery failed: " + e.getMessage(), clock.instant());
    }
    Run toSave = result;
    return transactions.execute(status -> runs.save(toSave));
  }
}
