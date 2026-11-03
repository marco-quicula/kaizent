package br.com.refatorado.kaizent.evaluation;

import br.com.refatorado.kaizent.intake.EvaluationProblems;
import br.com.refatorado.kaizent.intake.Problem;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmProfiles;
import br.com.refatorado.kaizent.run.EvaluationRuns;
import br.com.refatorado.kaizent.run.Run;
import br.com.refatorado.kaizent.run.RunStatus;
import br.com.refatorado.kaizent.run.Runs;
import br.com.refatorado.kaizent.workspace.EvaluationCase;
import br.com.refatorado.kaizent.workspace.Workspace;
import br.com.refatorado.kaizent.workspace.WorkspaceContent;
import br.com.refatorado.kaizent.workspace.WorkspaceLoader;
import br.com.refatorado.kaizent.workspace.WorkspaceProblem;
import br.com.refatorado.kaizent.workspace.WorkspaceValidator;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Runs every evaluation case of the workspace (spec 0003 AC-4, AC-5): stores the case's ticket as a
 * problem, starts an evaluation run ({@code ReviewMode.SKIP}), waits until the worker has finished
 * it, checks the expected properties and writes {@code report-<timestamp>.md}. Then it stops the
 * application. Cases run one after another, so each duration is the case's own.
 *
 * <p>It refuses to start with the fake adapters, which say nothing about a model, unless {@code
 * --dry-run} is given (used by its own tests).
 */
@Component
@Profile("eval")
@EnableConfigurationProperties(EvalProperties.class)
class EvalRunner implements ApplicationRunner {

  static final String DRY_RUN = "dry-run";

  private static final Logger log = LoggerFactory.getLogger(EvalRunner.class);
  private static final Set<RunStatus> FINISHED = Set.of(RunStatus.EVALUATED, RunStatus.FAILED);

  private final Workspace workspace;
  private final LlmProfiles profiles;
  private final EvaluationProblems problems;
  private final EvaluationRuns evaluationRuns;
  private final Runs runs;
  private final EvalProperties properties;
  private final Clock clock;
  private final ConfigurableApplicationContext context;

  EvalRunner(
      Workspace workspace,
      LlmProfiles profiles,
      EvaluationProblems problems,
      EvaluationRuns evaluationRuns,
      Runs runs,
      EvalProperties properties,
      Clock clock,
      ConfigurableApplicationContext context) {
    this.workspace = workspace;
    this.profiles = profiles;
    this.problems = problems;
    this.evaluationRuns = evaluationRuns;
    this.runs = runs;
    this.properties = properties;
    this.clock = clock;
    this.context = context;
  }

  @Override
  public void run(ApplicationArguments args) throws InterruptedException {
    boolean dryRun = args.containsOption(DRY_RUN);
    refuseFakeAdapters(profiles.byStep(), dryRun);
    Path report = evaluate(dryRun);
    log.info("Evaluation report: {}", report.toAbsolutePath());
    if (properties.exitWhenDone()) {
      System.exit(SpringApplication.exit(context));
    }
  }

  /** Runs all cases and writes the report. Returns the report's path. */
  Path evaluate(boolean dryRun) throws InterruptedException {
    Instant startedAt = clock.instant();
    WorkspaceContent content = WorkspaceLoader.load(workspace);
    List<WorkspaceProblem> invalid = WorkspaceValidator.validate(content);
    if (!invalid.isEmpty()) {
      throw new IllegalStateException(
          "The workspace is not valid, so it cannot be evaluated:\n  "
              + invalid.stream()
                  .map(WorkspaceProblem::toString)
                  .collect(Collectors.joining("\n  ")));
    }
    List<EvaluationCase> cases =
        content.evaluationCases().stream()
            .sorted(Comparator.comparing(EvaluationCase::ticket))
            .toList();
    Set<String> languages = new TreeSet<>();
    cases.forEach(c -> languages.add(c.expect().replyLanguage()));
    List<PropertyChecker> checkers = PropertyCheckers.all(languages);
    String source = "evaluation-" + EvalReportWriter.FILE_TIMESTAMP.format(startedAt);
    log.info("Evaluating {} cases as {}", cases.size(), source);

    List<CaseResult> results = new ArrayList<>();
    for (EvaluationCase evaluationCase : cases) {
      results.add(evaluate(evaluationCase, source, checkers));
    }
    return EvalReportWriter.write(
        properties.outputPath(),
        new EvalReportWriter.Session(
            startedAt, clock.instant(), workspace.root(), profiles.byStep(), dryRun),
        results);
  }

  private CaseResult evaluate(
      EvaluationCase evaluationCase, String source, List<PropertyChecker> checkers)
      throws InterruptedException {
    Instant start = clock.instant();
    Optional<Problem> problem = problems.store(source, evaluationCase.ticket());
    if (problem.isEmpty()) {
      return result(
          evaluationCase,
          Optional.empty(),
          "NOT STARTED",
          start,
          checkers,
          Outcome.failed("no ticket source has ticket " + evaluationCase.ticket()));
    }
    Run run = evaluationRuns.start(problem.get().id());
    Run finished = waitUntilFinished(run);
    String status = FINISHED.contains(finished.status()) ? finished.status().name() : "TIMED OUT";
    Outcome outcome =
        FINISHED.contains(finished.status())
            ? Outcome.of(finished)
            : Outcome.failed(
                "the run did not finish within "
                    + properties.caseTimeout()
                    + "; it is "
                    + finished.status());
    log.info("Case {}: {}", evaluationCase.ticket(), status);
    return result(evaluationCase, Optional.of(run.id()), status, start, checkers, outcome);
  }

  private Run waitUntilFinished(Run run) throws InterruptedException {
    Instant deadline = clock.instant().plus(properties.caseTimeout());
    Run current = run;
    while (!FINISHED.contains(current.status()) && clock.instant().isBefore(deadline)) {
      Thread.sleep(properties.pollInterval());
      current = runs.findById(run.id()).orElse(current);
    }
    return current;
  }

  private CaseResult result(
      EvaluationCase evaluationCase,
      Optional<java.util.UUID> runId,
      String status,
      Instant start,
      List<PropertyChecker> checkers,
      Outcome outcome) {
    Map<String, CheckResult> checks = new LinkedHashMap<>();
    for (PropertyChecker checker : checkers) {
      checks.put(checker.property(), checker.check(evaluationCase.expect(), outcome));
    }
    return new CaseResult(
        evaluationCase.ticket(),
        runId,
        status,
        Duration.between(start, clock.instant()),
        checks,
        outcome.suggestedReply());
  }

  /**
   * Fails unless every step uses a real adapter, or the run is a dry run (spec 0003 AC-5).
   *
   * @throws IllegalStateException naming the steps that use the fake adapter
   */
  static void refuseFakeAdapters(Map<String, LlmProfile> profiles, boolean dryRun) {
    if (dryRun) {
      return;
    }
    List<String> fake =
        profiles.entrySet().stream()
            .filter(entry -> LlmProfiles.FAKE_ADAPTER.equals(entry.getValue().adapter()))
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
    if (!fake.isEmpty()) {
      throw new IllegalStateException(
          "Evaluation needs real models, but these steps use the fake adapter: "
              + String.join(", ", fake)
              + ". Configure a real adapter (kaizent.llm.profiles.<step>.adapter), or pass --"
              + DRY_RUN
              + " to test the runner itself.");
    }
  }
}
