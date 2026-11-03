package br.com.refatorado.kaizent.evaluation;

import br.com.refatorado.kaizent.evaluation.CheckResult.Verdict;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmProfiles;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Writes {@code report-<timestamp>.md}: pass or fail per property and case, and totals. */
final class EvalReportWriter {

  static final DateTimeFormatter FILE_TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

  /** The run as a whole, for the report's header. */
  record Session(
      Instant startedAt,
      Instant finishedAt,
      Path workspace,
      Map<String, LlmProfile> profiles,
      boolean dryRun,
      Duration caseDelay) {}

  private EvalReportWriter() {}

  /** Writes the report into {@code folder} and returns its path. */
  static Path write(Path folder, Session session, List<CaseResult> cases) {
    Path file = folder.resolve("report-" + FILE_TIMESTAMP.format(session.startedAt()) + ".md");
    try {
      Files.createDirectories(folder);
      Files.writeString(file, render(session, cases), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot write the evaluation report " + file, e);
    }
    return file;
  }

  static String render(Session session, List<CaseResult> cases) {
    Set<String> properties = new LinkedHashSet<>();
    cases.forEach(c -> properties.addAll(c.results().keySet()));
    StringBuilder out = new StringBuilder();
    out.append("# Evaluation report\n\n");
    List<String> fakeSteps =
        session.profiles().entrySet().stream()
            .filter(entry -> LlmProfiles.FAKE_ADAPTER.equals(entry.getValue().adapter()))
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
    if (!fakeSteps.isEmpty()) {
      out.append("> **Dry run.** These steps used the fake adapter, which answers with scripted")
          .append(" replies: ")
          .append(String.join(", ", fakeSteps))
          .append(". Their results say nothing about the quality of any model.\n\n");
    } else if (session.dryRun()) {
      out.append("> **Dry run** (`--dry-run`), but every step used a real adapter, so these")
          .append(" results come from the configured models.\n\n");
    }
    out.append("- **Started:** ").append(session.startedAt()).append('\n');
    out.append("- **Duration:** ")
        .append(format(Duration.between(session.startedAt(), session.finishedAt())))
        .append('\n');
    out.append("- **Workspace:** `").append(session.workspace()).append("`\n");
    session.profiles().entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry ->
            out.append("- **Step `")
                .append(entry.getKey())
                .append("`:** adapter `")
                .append(entry.getValue().adapter())
                .append("`, model `")
                .append(entry.getValue().model())
                .append("`\n"));
    if (session.caseDelay().isPositive()) {
      out.append("- **Delay between cases:** ").append(format(session.caseDelay())).append('\n');
    }
    out.append("- **Tokens and cost:** not recorded yet; usage tracking arrives with specs 0004")
        .append(" and 0009.\n\n");

    out.append("## Totals\n\n");
    out.append("| Property | Pass | Fail | Skipped |\n|---|---|---|---|\n");
    long pass = 0;
    long fail = 0;
    long skipped = 0;
    for (String property : properties) {
      long p = count(cases, property, Verdict.PASS);
      long f = count(cases, property, Verdict.FAIL);
      long s = count(cases, property, Verdict.SKIPPED);
      pass += p;
      fail += f;
      skipped += s;
      out.append("| `")
          .append(property)
          .append("` | ")
          .append(p)
          .append(" | ")
          .append(f)
          .append(" | ")
          .append(s)
          .append(" |\n");
    }
    out.append("| **All** | **")
        .append(pass)
        .append("** | **")
        .append(fail)
        .append("** | **")
        .append(skipped)
        .append("** |\n\n");

    out.append("## Cases\n\n");
    out.append("| Case | Run status | Duration |");
    properties.forEach(property -> out.append(" `").append(property).append("` |"));
    out.append("\n|---|---|---|");
    properties.forEach(property -> out.append("---|"));
    out.append('\n');
    for (CaseResult result : cases) {
      out.append("| ")
          .append(result.ticket())
          .append(" | ")
          .append(result.runStatus())
          .append(" | ")
          .append(format(result.duration()))
          .append(" |");
      for (String property : properties) {
        CheckResult check = result.results().get(property);
        out.append(' ').append(check == null ? "" : symbol(check.verdict())).append(" |");
      }
      out.append('\n');
    }
    out.append("\n✅ pass · ❌ fail · ⏭️ skipped (the pipeline does not produce it yet, or the case")
        .append(" was not run)\n\n");

    out.append("## Details\n");
    for (CaseResult result : cases) {
      out.append("\n### ").append(result.ticket()).append("\n\n");
      result.runId().ifPresent(id -> out.append("- **Run:** `").append(id).append("`\n"));
      result
          .results()
          .forEach((property, check) -> {
            out.append("- `").append(property).append("`: ").append(symbol(check.verdict()));
            if (!check.detail().isBlank()) {
              out.append(' ').append(check.detail());
            }
            out.append('\n');
          });
      result
          .suggestedReply()
          .ifPresent(reply -> {
            out.append("\nSuggested reply:\n\n");
            reply.strip().lines().forEach(line -> out.append("> ").append(line).append('\n'));
          });
    }
    return out.toString();
  }

  private static long count(List<CaseResult> cases, String property, Verdict verdict) {
    return cases.stream()
        .map(c -> c.results().get(property))
        .filter(check -> check != null && check.verdict() == verdict)
        .count();
  }

  private static String symbol(Verdict verdict) {
    return switch (verdict) {
      case PASS -> "✅";
      case FAIL -> "❌";
      case SKIPPED -> "⏭️";
    };
  }

  static String format(Duration duration) {
    long millis = duration.toMillis();
    return millis < 1000
        ? millis + " ms"
        : String.format(java.util.Locale.ROOT, "%.1f s", millis / 1000.0);
  }
}
