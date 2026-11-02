package br.com.refatorado.kaizent;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.llm.LlmAdapter;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.resolution.Proposal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.FileSystemUtils;

/**
 * With the real schedulers on: intake keeps polling while the worker waits for a slow model call
 * (spec 0002 AC-1). The context is closed afterwards, so its schedulers do not touch other tests.
 */
@KaizentIntegrationTest
@TestPropertySource(
    properties = {
      "kaizent.scheduling.enabled=true",
      "kaizent.intake.poll-interval=100ms",
      "kaizent.worker.poll-interval=100ms",
      "kaizent.workspace.path=" + SchedulingTests.WORKSPACE,
      "kaizent.llm.profiles.resolution.adapter=" + SchedulingTests.BlockingAdapter.NAME
    })
@Import(SchedulingTests.BlockingAdapterConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SchedulingTests {

  static final String WORKSPACE = "target/scheduling-tests-workspace";

  @Autowired private JdbcTemplate jdbc;

  @BeforeAll
  static void createWorkspace() throws IOException {
    FileSystemUtils.deleteRecursively(Path.of(WORKSPACE));
    Files.createDirectories(Path.of(WORKSPACE, "inbox"));
  }

  @AfterEach
  void releaseTheModel() {
    BlockingAdapter.release.countDown();
  }

  @Test
  void intakeKeepsPollingWhileTheWorkerWaitsForTheModel() throws Exception {
    ticket("SCHED-1");
    assertThat(BlockingAdapter.entered.await(15, TimeUnit.SECONDS))
        .as("the worker is inside the model call")
        .isTrue();

    ticket("SCHED-2");

    assertThat(waitForProblem("SCHED-2", Duration.ofSeconds(5)))
        .as("SCHED-2 is received while SCHED-1 is still resolving")
        .isTrue();
  }

  private static void ticket(String key) throws IOException {
    Files.writeString(Path.of(WORKSPACE, "inbox", key + ".md"), "Ticket " + key + "\n");
  }

  private boolean waitForProblem(String key, Duration timeout) throws InterruptedException {
    Instant deadline = Instant.now().plus(timeout);
    while (Instant.now().isBefore(deadline)) {
      Integer count =
          jdbc.queryForObject(
              "SELECT count(*) FROM problem WHERE external_key = ?", Integer.class, key);
      if (count != null && count > 0) {
        return true;
      }
      Thread.sleep(50);
    }
    return false;
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class BlockingAdapterConfiguration {
    @Bean
    BlockingAdapter blockingAdapter() {
      return new BlockingAdapter();
    }
  }

  /** A model call that does not return until the test releases it. */
  static class BlockingAdapter implements LlmAdapter {

    static final String NAME = "blocking";
    static final CountDownLatch entered = new CountDownLatch(1);
    static final CountDownLatch release = new CountDownLatch(1);

    @Override
    public String name() {
      return NAME;
    }

    @Override
    public <T> LlmResponse<T> complete(LlmRequest<T> request, LlmProfile profile) {
      entered.countDown();
      try {
        release.await(30, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return new LlmResponse<>(request.outputType().cast(new Proposal("d", "r", 0.1)), "{}");
    }
  }
}
