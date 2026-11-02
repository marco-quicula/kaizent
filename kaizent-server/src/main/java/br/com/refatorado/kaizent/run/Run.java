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
    RunStatus status,
    FailureReason failureReason,
    String failureMessage,
    Proposal proposal,
    Instant createdAt,
    Instant updatedAt,
    @Version Integer version) {

  static final int MAX_FAILURE_MESSAGE_LENGTH = 2000;

  /** A new run for a received problem. */
  public static Run received(UUID problemId, Instant now) {
    return new Run(
        UUID.randomUUID(), problemId, RunStatus.RECEIVED, null, null, null, now, now, null);
  }

  public Run startResolving(Instant now) {
    requireStatus(RunStatus.RECEIVED);
    return with(RunStatus.RESOLVING, null, null, proposal, now);
  }

  public Run awaitReview(Proposal newProposal, Instant now) {
    requireStatus(RunStatus.RESOLVING);
    return with(RunStatus.AWAITING_REVIEW, null, null, newProposal, now);
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
    return new Run(id, problemId, newStatus, reason, message, newProposal, createdAt, now, version);
  }

  private void requireStatus(RunStatus expected) {
    if (status != expected) {
      throw new IllegalStateException("Run " + id + " is " + status + ", expected " + expected);
    }
  }
}
