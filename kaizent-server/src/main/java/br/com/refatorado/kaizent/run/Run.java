package br.com.refatorado.kaizent.run;

import br.com.refatorado.kaizent.resolution.Proposal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/**
 * One attempt to resolve a problem, moving through the state machine of ADR-0023. Instances are
 * immutable; each transition returns a new instance that the caller saves.
 */
@Table("run")
public record Run(
    @Id UUID id,
    UUID problemId,
    ReviewMode reviewMode,
    RunStatus status,
    FailureReason failureReason,
    String failureMessage,
    Proposal proposal,
    Verdict verdict,
    Instant createdAt,
    Instant updatedAt,
    @Version Integer version) {

  static final int MAX_FAILURE_MESSAGE_LENGTH = 2000;

  /** A new run for a received problem, reviewed by a human. */
  public static Run received(UUID problemId, Instant now) {
    return received(problemId, ReviewMode.HUMAN, now);
  }

  /** A new run for a received problem, with the given review mode. */
  public static Run received(UUID problemId, ReviewMode reviewMode, Instant now) {
    return new Run(
        UUID.randomUUID(),
        problemId,
        reviewMode,
        RunStatus.RECEIVED,
        null,
        null,
        null,
        null,
        now,
        now,
        null);
  }

  public Run startResolving(Instant now) {
    requireStatus(RunStatus.RECEIVED);
    return with(RunStatus.RESOLVING, null, null, proposal, now);
  }

  /** The proposal of a {@link ReviewMode#HUMAN} run waits for a verdict. */
  public Run awaitReview(Proposal newProposal, Instant now) {
    requireStatus(RunStatus.RESOLVING);
    requireReviewMode(ReviewMode.HUMAN);
    return with(RunStatus.AWAITING_REVIEW, null, null, newProposal, now);
  }

  /** The proposal of a {@link ReviewMode#SKIP} run ends it, with no review and no delivery. */
  public Run evaluated(Proposal newProposal, Instant now) {
    requireStatus(RunStatus.RESOLVING);
    requireReviewMode(ReviewMode.SKIP);
    return with(RunStatus.EVALUATED, null, null, newProposal, now);
  }

  /** Ends resolution: {@link #awaitReview} or {@link #evaluated}, by the review mode. */
  public Run resolved(Proposal newProposal, Instant now) {
    return reviewMode == ReviewMode.SKIP
        ? evaluated(newProposal, now)
        : awaitReview(newProposal, now);
  }

  /** Records an {@code APPROVED} verdict on a run awaiting review. */
  public Run approve(Verdict newVerdict, Instant now) {
    requireStatus(RunStatus.AWAITING_REVIEW);
    if (newVerdict.type() != VerdictType.APPROVED) {
      throw new IllegalArgumentException("Not an APPROVED verdict: " + newVerdict.type());
    }
    return new Run(
        id,
        problemId,
        reviewMode,
        RunStatus.APPROVED,
        null,
        null,
        proposal,
        newVerdict,
        createdAt,
        now,
        version);
  }

  /** The approved reply was delivered. */
  public Run delivered(Instant now) {
    requireStatus(RunStatus.APPROVED);
    return with(RunStatus.DELIVERED, null, null, proposal, now);
  }

  public Run fail(FailureReason reason, String message, Instant now) {
    String text = message == null || message.isBlank() ? "Unknown error" : message.strip();
    if (text.length() > MAX_FAILURE_MESSAGE_LENGTH) {
      text = text.substring(0, MAX_FAILURE_MESSAGE_LENGTH);
    }
    return with(RunStatus.FAILED, reason, text, proposal, now);
  }

  private Run with(
      RunStatus newStatus,
      FailureReason reason,
      String message,
      Proposal newProposal,
      Instant now) {
    return new Run(
        id,
        problemId,
        reviewMode,
        newStatus,
        reason,
        message,
        newProposal,
        verdict,
        createdAt,
        now,
        version);
  }

  private void requireStatus(RunStatus expected) {
    if (status != expected) {
      throw new RunStateException(id, status, expected);
    }
  }

  private void requireReviewMode(ReviewMode expected) {
    if (reviewMode != expected) {
      throw new IllegalStateException(
          "Run " + id + " has review mode " + reviewMode + ", expected " + expected);
    }
  }
}
