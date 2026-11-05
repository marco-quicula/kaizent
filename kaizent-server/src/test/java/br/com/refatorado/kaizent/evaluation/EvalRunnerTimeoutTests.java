package br.com.refatorado.kaizent.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.TestcontainersConfiguration;
import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmAdapter;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.resolution.ResolutionOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.FileSystemUtils;

/**
 * A case whose model call does not finish in time stops the session: the single worker is still
 * busy with it, so the remaining cases are reported as not run instead of timing out behind it.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    properties = {
      "kaizent.scheduling.enabled=true",
      "kaizent.worker.poll-interval=50ms",
      "kaizent.workspace.path=../examples/petclinic",
      "kaizent.llm.profiles.resolution.adapter=" + EvalRunnerTimeoutTests.StalledAdapter.NAME,
      "kaizent.eval.output-path=" + EvalRunnerTimeoutTests.OUTPUT,
      "kaizent.eval.poll-interval=50ms",
      "kaizent.eval.case-timeout=500ms",
      "kaizent.eval.exit-when-done=false"
    })
@Import({
  TestcontainersConfiguration.class,
  EvalRunnerTimeoutTests.StalledAdapterConfiguration.class
})
@ActiveProfiles({"test", "eval"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EvalRunnerTimeoutTests {

  static final String OUTPUT = "target/eval-runner-timeout-tests";

  @BeforeAll
  static void cleanOutput() {
    FileSystemUtils.deleteRecursively(Path.of(OUTPUT).toFile());
  }

  @AfterAll
  static void releaseTheModel() {
    StalledAdapter.release.countDown();
  }

  @Test
  void stopsAfterACaseTimesOutAndReportsTheRestAsNotRun() throws IOException {
    List<Path> reports;
    try (Stream<Path> files = Files.list(Path.of(OUTPUT))) {
      reports = files.toList();
    }
    String report = Files.readString(reports.getFirst());

    assertThat(report)
        .contains("| PC-001 | TIMED OUT |")
        // RECEIVED when the worker has not picked the run up yet (a slow machine), RESOLVING
        // when the stalled model call has started: either way the run is unfinished.
        .containsPattern(
            "- `mentionsAll`: ❌ the run did not finish within PT0\\.5S; it is (RECEIVED|RESOLVING)")
        .contains("| `mentionsAll` | 0 | 1 | 11 |")
        .contains("- `mentionsAll`: ⏭️ not run: the session stopped after PC-001 timed out");
    for (int i = 2; i <= 12; i++) {
      assertThat(report).contains("| PC-%03d | NOT RUN |".formatted(i));
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class StalledAdapterConfiguration {
    @Bean
    StalledAdapter stalledAdapter() {
      return new StalledAdapter();
    }
  }

  /** A model call that does not return until the test class is done. */
  static class StalledAdapter implements LlmAdapter {

    static final String NAME = "stalled";
    static final CountDownLatch release = new CountDownLatch(1);

    @Override
    public String name() {
      return NAME;
    }

    @Override
    public CostType costType() {
      return CostType.UNKNOWN;
    }

    @Override
    public <T> LlmResponse<T> complete(LlmRequest<T> request, LlmProfile profile) {
      try {
        release.await(60, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return new LlmResponse<>(
          request.outputType().cast(new ResolutionOutput("d", "r", 0.1)),
          Usage.unknown(name(), profile.model()),
          "{}");
    }
  }
}
