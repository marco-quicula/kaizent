package br.com.refatorado.kaizent.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.TestcontainersConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.FileSystemUtils;

/**
 * Spec 0003 AC-4 and AC-5: with {@code --dry-run}, the runner evaluates every PetClinic case with
 * the fake adapters, through the real worker, and writes the report. The context is closed
 * afterwards, so its worker does not touch other tests.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    args = "--" + EvalRunner.DRY_RUN,
    properties = {
      "kaizent.scheduling.enabled=true",
      "kaizent.worker.poll-interval=50ms",
      "kaizent.workspace.path=../examples/petclinic",
      "kaizent.eval.output-path=" + EvalRunnerTests.OUTPUT,
      "kaizent.eval.poll-interval=50ms",
      "kaizent.eval.case-timeout=30s",
      "kaizent.eval.exit-when-done=false"
    })
@Import(TestcontainersConfiguration.class)
@ActiveProfiles({"test", "eval"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EvalRunnerTests {

  static final String OUTPUT = "target/eval-runner-tests";

  /** This class's session; other test classes may have run evaluations before. */
  private static final String LATEST_SESSION =
      "SELECT max(source) FROM problem WHERE source LIKE 'evaluation-%'";

  @Autowired private JdbcTemplate jdbc;

  @BeforeAll
  static void cleanOutput() {
    FileSystemUtils.deleteRecursively(Path.of(OUTPUT).toFile());
  }

  @Test
  void evaluatesEveryCaseAndWritesTheReport() throws IOException {
    List<Path> reports;
    try (Stream<Path> files = Files.list(Path.of(OUTPUT))) {
      reports = files.toList();
    }
    assertThat(reports).hasSize(1);
    String report = Files.readString(reports.getFirst());

    assertThat(report)
        .contains("> **Dry run.**")
        .contains("adapter `fake`")
        .contains("| `subject` | 0 | 0 | 12 |")
        .contains("| `replyLanguage` | 11 | 1 | 0 |")
        .contains("- `replyLanguage`: ❌ the reply is in en, expected pt");
    for (int i = 1; i <= 12; i++) {
      assertThat(report).contains("| PC-%03d | EVALUATED |".formatted(i));
    }
  }

  @Test
  void usesEvaluationRunsOnly() {
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM run r JOIN problem p ON p.id = r.problem_id"
                    + " WHERE p.source = ("
                    + LATEST_SESSION
                    + ") AND r.review_mode = 'SKIP' AND r.status = 'EVALUATED'",
                Integer.class))
        .isEqualTo(12);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM run r JOIN problem p ON p.id = r.problem_id"
                    + " WHERE p.source LIKE 'evaluation-%' AND r.review_mode <> 'SKIP'",
                Integer.class))
        .isZero();
  }

  @Test
  void deliversNoReply() throws IOException {
    try (Stream<Path> files = Files.list(Path.of("../examples/petclinic/outbox"))) {
      assertThat(files.map(Path::getFileName).map(Path::toString))
          .noneMatch(name -> name.endsWith(".reply.md"));
    }
  }
}
