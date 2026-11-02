package br.com.refatorado.kaizent.run.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The single worker of ADR-0023: processes runs one at a time until none is waiting. Off when
 * {@code kaizent.scheduling.enabled=false} (tests drive the orchestrator directly).
 */
@Component
@ConditionalOnProperty(
    name = "kaizent.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true)
class RunWorker {

  private final RunOrchestrator orchestrator;

  RunWorker(RunOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @Scheduled(fixedDelayString = "${kaizent.worker.poll-interval:2s}")
  void work() {
    while (orchestrator.processNext().isPresent()) {
      // Keep going while runs are waiting.
    }
  }
}
