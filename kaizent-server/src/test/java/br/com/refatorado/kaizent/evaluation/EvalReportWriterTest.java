package br.com.refatorado.kaizent.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.llm.LlmProfile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Spec 0003 AC-4: the report has pass or fail per property and case, and totals. */
class EvalReportWriterTest {

  private static final Instant START = Instant.parse("2026-11-03T15:00:00Z");

  @TempDir private Path folder;

  @Test
  void writesTotalsCasesAndDetails() throws IOException {
    Map<String, CheckResult> first = new LinkedHashMap<>();
    first.put("subject", CheckResult.skipped("not produced until spec 0006 (triage)"));
    first.put("mentionsAll", CheckResult.pass(""));
    first.put("replyLanguage", CheckResult.pass("en"));
    Map<String, CheckResult> second = new LinkedHashMap<>();
    second.put("subject", CheckResult.skipped("not produced until spec 0006 (triage)"));
    second.put("mentionsAll", CheckResult.fail("missing \"endereço\""));
    second.put("replyLanguage", CheckResult.fail("the reply is in en, expected pt"));
    UUID runId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    List<CaseResult> cases =
        List.of(
            new CaseResult(
                "PC-001",
                Optional.of(runId),
                "EVALUATED",
                Duration.ofMillis(1500),
                first,
                Optional.of("Use ten digits.")),
            new CaseResult(
                "PC-003",
                Optional.empty(),
                "FAILED",
                Duration.ofMillis(200),
                second,
                Optional.empty()));
    EvalReportWriter.Session session =
        new EvalReportWriter.Session(
            START,
            START.plusSeconds(3),
            Path.of("/repo/examples/petclinic"),
            Map.of("resolution", new LlmProfile("gemini-api", "gemini-3.6-flash", null)),
            false,
            Duration.ofSeconds(15));

    Path report = EvalReportWriter.write(folder, session, cases);

    assertThat(report).isEqualTo(folder.resolve("report-20261103T150000Z.md"));
    String text = Files.readString(report);
    assertThat(text)
        .startsWith("# Evaluation report\n\n- **Started:** 2026-11-03T15:00:00Z\n")
        .doesNotContain("Dry run")
        .contains("- **Duration:** 3.0 s\n")
        .contains("- **Step `resolution`:** adapter `gemini-api`, model `gemini-3.6-flash`\n")
        .contains("- **Delay between cases:** 15.0 s\n")
        .contains("- **Tokens and cost:** not recorded yet")
        .contains("| `subject` | 0 | 0 | 2 |\n")
        .contains("| `mentionsAll` | 1 | 1 | 0 |\n")
        .contains("| **All** | **2** | **2** | **2** |\n")
        .contains("| PC-001 | EVALUATED | 1.5 s | ⏭️ | ✅ | ✅ |\n")
        .contains("| PC-003 | FAILED | 200 ms | ⏭️ | ❌ | ❌ |\n")
        .contains("- **Run:** `" + runId + "`\n")
        .contains("- `replyLanguage`: ❌ the reply is in en, expected pt\n")
        .contains("Suggested reply:\n\n> Use ten digits.\n");
  }

  @Test
  void namesTheStepsThatUsedTheFakeAdapter() {
    EvalReportWriter.Session session =
        new EvalReportWriter.Session(
            START,
            START,
            Path.of("ws"),
            Map.of(
                "resolution", new LlmProfile("fake", null, null),
                "triage", new LlmProfile("gemini-api", "gemini-3.6-flash", null)),
            true,
            Duration.ZERO);

    assertThat(EvalReportWriter.render(session, List.of()))
        .contains(
            "> **Dry run.** These steps used the fake adapter, which answers with scripted"
                + " replies: resolution.");
  }

  @Test
  void doesNotClaimFakeAnswersInADryRunWithRealAdapters() {
    EvalReportWriter.Session session =
        new EvalReportWriter.Session(
            START,
            START,
            Path.of("ws"),
            Map.of("resolution", new LlmProfile("gemini-api", "gemini-3.6-flash", null)),
            true,
            Duration.ZERO);

    assertThat(EvalReportWriter.render(session, List.of()))
        .contains("> **Dry run** (`--dry-run`), but every step used a real adapter")
        .doesNotContain("Delay between cases")
        .doesNotContain("scripted");
  }
}
