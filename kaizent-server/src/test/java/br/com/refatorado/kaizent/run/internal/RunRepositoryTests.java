package br.com.refatorado.kaizent.run.internal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.Database;
import br.com.refatorado.kaizent.KaizentIntegrationTest;
import br.com.refatorado.kaizent.intake.IncomingTicket;
import br.com.refatorado.kaizent.intake.Problem;
import br.com.refatorado.kaizent.intake.internal.IntakeService;
import br.com.refatorado.kaizent.resolution.Proposal;
import br.com.refatorado.kaizent.run.Run;
import br.com.refatorado.kaizent.run.RunStatus;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** Spec 0002 T2: the run table, its jsonb proposal, optimistic locking and the worker claim. */
@KaizentIntegrationTest
class RunRepositoryTests {

  @Autowired private RunRepository runs;
  @Autowired private IntakeService intake;
  @Autowired private TransactionTemplate transactions;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    Database.clear(jdbc);
  }

  @Test
  void storesTheProposalAsJsonAndIncrementsTheVersion() {
    Run run = runFor("PC-200");
    assertThat(run.version()).isZero();

    Run saved =
        runs.save(
            run.startResolving(Instant.now())
                .awaitReview(new Proposal("Diagnosis", "Reply", 0.75), Instant.now()));

    Run loaded = runs.findById(run.id()).orElseThrow();
    assertThat(loaded.status()).isEqualTo(RunStatus.AWAITING_REVIEW);
    assertThat(loaded.proposal()).isEqualTo(new Proposal("Diagnosis", "Reply", 0.75));
    assertThat(loaded.version()).isEqualTo(saved.version()).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT proposal->>'suggestedReply' FROM run", String.class))
        .isEqualTo("Reply");
  }

  @Test
  void locksTheOldestReceivedRunAndSkipsLockedOnes() throws Exception {
    Run oldest = runFor("PC-201");
    Run next = runFor("PC-202");

    Run lockedByOther =
        transactions.execute(status -> {
          Run first = runs.lockNextReceived().orElseThrow();
          // A second worker, in its own transaction, must skip the locked run.
          Run second =
              CompletableFuture.supplyAsync(
                      () -> transactions.execute(s -> runs.lockNextReceived().orElseThrow()))
                  .join();
          assertThat(second.id()).isEqualTo(next.id());
          return first;
        });

    assertThat(lockedByOther.id()).isEqualTo(oldest.id());
  }

  private Run runFor(String key) {
    Problem problem =
        intake
            .ingest(
                "test",
                new IncomingTicket(
                    key, key, "Body", null, null, Instant.now(), List.of(), "h", null))
            .orElseThrow();
    return runs.findByProblemId(problem.id()).getFirst();
  }
}
