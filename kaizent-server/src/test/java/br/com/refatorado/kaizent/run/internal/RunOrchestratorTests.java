package br.com.refatorado.kaizent.run.internal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.Database;
import br.com.refatorado.kaizent.KaizentIntegrationTest;
import br.com.refatorado.kaizent.intake.IncomingTicket;
import br.com.refatorado.kaizent.intake.internal.IntakeService;
import br.com.refatorado.kaizent.run.FailureReason;
import br.com.refatorado.kaizent.run.ReviewMode;
import br.com.refatorado.kaizent.run.Run;
import br.com.refatorado.kaizent.run.RunStatus;
import br.com.refatorado.kaizent.workspace.Workspace;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileSystemUtils;

/** Spec 0002 AC-3 and AC-4, and spec 0003 T6, with the fake LLM adapter. */
@KaizentIntegrationTest
class RunOrchestratorTests {

  @Autowired private RunOrchestrator orchestrator;
  @Autowired private RunRepository runs;
  @Autowired private IntakeService intake;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Workspace workspace;

  @BeforeEach
  void clean() {
    Database.clear(jdbc);
    FileSystemUtils.deleteRecursively(workspace.outbox().toFile());
  }

  @Test
  void resolvesAReceivedRunIntoAProposalAwaitingReview() {
    receive("PC-001", Instant.parse("2026-10-01T10:00:00Z"));

    Run run = orchestrator.processNext().orElseThrow();

    assertThat(run.status()).isEqualTo(RunStatus.AWAITING_REVIEW);
    Run stored = runs.findById(run.id()).orElseThrow();
    assertThat(stored.status()).isEqualTo(RunStatus.AWAITING_REVIEW);
    assertThat(stored.proposal().diagnosis()).startsWith("Fake diagnosis");
    assertThat(stored.proposal().suggestedReply()).isNotBlank();
    assertThat(stored.proposal().rawConfidence()).isEqualTo(0.5);
    assertThat(stored.failureReason()).isNull();
  }

  @Test
  void failsTheRunWhenTheModelCallFailsAndContinuesWithTheNextOne() {
    receive("PC-ERR", Instant.parse("2026-10-01T10:00:00Z"));
    receive("PC-001", Instant.parse("2026-10-01T10:05:00Z"));

    Run failed = orchestrator.processNext().orElseThrow();
    Run next = orchestrator.processNext().orElseThrow();

    assertThat(failed.status()).isEqualTo(RunStatus.FAILED);
    assertThat(failed.failureReason()).isEqualTo(FailureReason.ERROR);
    assertThat(failed.failureMessage()).contains("Fake LLM error");
    assertThat(failed.proposal()).isNull();
    assertThat(next.status()).isEqualTo(RunStatus.AWAITING_REVIEW);
    assertThat(next.id()).isNotEqualTo(failed.id());
    assertThat(orchestrator.processNext()).isEmpty();
  }

  @Test
  void endsAnEvaluationRunAsEvaluatedWithoutReviewOrDelivery() {
    receive("PC-001", Instant.parse("2026-10-01T10:00:00Z"));
    Database.makeEvaluationRuns(jdbc, "PC-001");

    Run run = orchestrator.processNext().orElseThrow();

    assertThat(run.status()).isEqualTo(RunStatus.EVALUATED);
    Run stored = runs.findById(run.id()).orElseThrow();
    assertThat(stored.status()).isEqualTo(RunStatus.EVALUATED);
    assertThat(stored.reviewMode()).isEqualTo(ReviewMode.SKIP);
    assertThat(stored.proposal().suggestedReply()).isNotBlank();
    assertThat(stored.verdict()).isNull();
    assertThat(workspace.outbox()).doesNotExist();
    assertThat(orchestrator.processNext()).isEmpty();
  }

  @Test
  void doesNothingWhenNoRunIsWaiting() {
    assertThat(orchestrator.processNext()).isEmpty();
  }

  private void receive(String key, Instant created) {
    intake.ingest(
        "test",
        new IncomingTicket(key, "Title " + key, "Body", null, null, created, List.of(), "h", null));
  }
}
