package br.com.refatorado.kaizent.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import br.com.refatorado.kaizent.resolution.Proposal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The transitions of ADR-0023 used by specs 0002 and 0003. */
class RunTest {

  private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");
  private static final Proposal PROPOSAL = new Proposal("d", "reply", 0.5);
  private static final Verdict APPROVED =
      new Verdict(VerdictType.APPROVED, "reply", null, "local", NOW);

  @Test
  void goesFromReceivedToDelivered() {
    Run run =
        Run.received(UUID.randomUUID(), NOW)
            .startResolving(NOW)
            .awaitReview(PROPOSAL, NOW)
            .approve(APPROVED, NOW)
            .delivered(NOW);

    assertThat(run.status()).isEqualTo(RunStatus.DELIVERED);
    assertThat(run.proposal()).isEqualTo(PROPOSAL);
    assertThat(run.verdict()).isEqualTo(APPROVED);
  }

  @Test
  void anEvaluationRunEndsAsEvaluatedAndCannotBeApproved() {
    Run run =
        Run.received(UUID.randomUUID(), ReviewMode.SKIP, NOW)
            .startResolving(NOW)
            .resolved(PROPOSAL, NOW);

    assertThat(run.status()).isEqualTo(RunStatus.EVALUATED);
    assertThat(run.reviewMode()).isEqualTo(ReviewMode.SKIP);
    assertThat(run.proposal()).isEqualTo(PROPOSAL);
    assertThatExceptionOfType(RunStateException.class)
        .isThrownBy(() -> run.approve(APPROVED, NOW))
        .satisfies(e -> assertThat(e.status()).isEqualTo(RunStatus.EVALUATED));
  }

  @Test
  void aHumanRunAwaitsReviewAndIsNeverEvaluated() {
    Run resolving = Run.received(UUID.randomUUID(), NOW).startResolving(NOW);

    assertThat(resolving.reviewMode()).isEqualTo(ReviewMode.HUMAN);
    assertThat(resolving.resolved(PROPOSAL, NOW).status()).isEqualTo(RunStatus.AWAITING_REVIEW);
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> resolving.evaluated(PROPOSAL, NOW));
  }

  @Test
  void anEvaluationRunNeverAwaitsReview() {
    Run resolving = Run.received(UUID.randomUUID(), ReviewMode.SKIP, NOW).startResolving(NOW);

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> resolving.awaitReview(PROPOSAL, NOW));
  }

  @Test
  void approvesOnlyARunAwaitingReview() {
    Run received = Run.received(UUID.randomUUID(), NOW);

    assertThatExceptionOfType(RunStateException.class)
        .isThrownBy(() -> received.approve(APPROVED, NOW))
        .satisfies(e -> assertThat(e.status()).isEqualTo(RunStatus.RECEIVED));
  }

  @Test
  void failingKeepsTheProposalAndVerdictAndShortensLongMessages() {
    Run approved =
        Run.received(UUID.randomUUID(), NOW)
            .startResolving(NOW)
            .awaitReview(PROPOSAL, NOW)
            .approve(APPROVED, NOW);

    Run failed = approved.fail(FailureReason.ERROR, "x".repeat(5000), NOW);

    assertThat(failed.status()).isEqualTo(RunStatus.FAILED);
    assertThat(failed.verdict()).isEqualTo(APPROVED);
    assertThat(failed.failureMessage()).hasSize(Run.MAX_FAILURE_MESSAGE_LENGTH);
  }
}
