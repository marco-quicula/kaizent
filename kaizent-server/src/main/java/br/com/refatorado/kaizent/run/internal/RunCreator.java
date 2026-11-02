package br.com.refatorado.kaizent.run.internal;

import br.com.refatorado.kaizent.intake.ProblemReceived;
import br.com.refatorado.kaizent.run.Run;
import java.time.Clock;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Creates a {@code RECEIVED} run for each new problem. It listens synchronously, so the run is
 * stored in the intake transaction: a problem and its run are committed together or not at all.
 */
@Component
class RunCreator {

  private final RunRepository runs;
  private final Clock clock;

  RunCreator(RunRepository runs, Clock clock) {
    this.runs = runs;
    this.clock = clock;
  }

  @EventListener
  void on(ProblemReceived event) {
    runs.save(Run.received(event.problemId(), clock.instant()));
  }
}
