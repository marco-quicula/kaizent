package br.com.refatorado.kaizent.run.internal;

import br.com.refatorado.kaizent.intake.Problem;
import br.com.refatorado.kaizent.intake.Problems;
import br.com.refatorado.kaizent.resolution.Proposal;
import br.com.refatorado.kaizent.resolution.SkeletonResolver;
import br.com.refatorado.kaizent.run.FailureReason;
import br.com.refatorado.kaizent.run.Run;
import java.time.Clock;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Takes the next {@code RECEIVED} run through resolution (ADR-0017, ADR-0023). The proposal then
 * waits for a verdict or, for an evaluation run ({@code ReviewMode.SKIP}), ends the run as {@code
 * EVALUATED}, with no review and no delivery. The model call runs outside any transaction; each
 * state change is committed on its own. Any error fails the run with reason {@code ERROR}, and the
 * worker moves on.
 */
@Component
public class RunOrchestrator {

  private static final Logger log = LoggerFactory.getLogger(RunOrchestrator.class);

  private final RunRepository runs;
  private final Problems problems;
  private final SkeletonResolver resolver;
  private final TransactionTemplate transactions;
  private final Clock clock;

  RunOrchestrator(
      RunRepository runs,
      Problems problems,
      SkeletonResolver resolver,
      TransactionTemplate transactions,
      Clock clock) {
    this.runs = runs;
    this.problems = problems;
    this.resolver = resolver;
    this.transactions = transactions;
    this.clock = clock;
  }

  /** Processes the oldest {@code RECEIVED} run, if any. Returns the run in its final state. */
  public Optional<Run> processNext() {
    Optional<Run> claimed =
        transactions.execute(status ->
            runs.lockNextReceived().map(run -> runs.save(run.startResolving(clock.instant()))));
    return claimed.map(this::resolve);
  }

  private Run resolve(Run run) {
    Run result;
    try {
      Problem problem =
          problems
              .findById(run.problemId())
              .orElseThrow(
                  () -> new IllegalStateException("Problem not found: " + run.problemId()));
      Proposal proposal = resolver.resolve(problem);
      result = run.resolved(proposal, clock.instant());
      log.info("Run {} for {} is {}", run.id(), problem.externalKey(), result.status());
    } catch (RuntimeException e) {
      log.warn("Run {} failed: {}", run.id(), e.getMessage());
      result = run.fail(FailureReason.ERROR, e.getMessage(), clock.instant());
    }
    Run toSave = result;
    return transactions.execute(status -> runs.save(toSave));
  }
}
