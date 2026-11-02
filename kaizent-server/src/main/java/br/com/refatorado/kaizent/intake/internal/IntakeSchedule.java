package br.com.refatorado.kaizent.intake.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the ticket sources every {@code kaizent.intake.poll-interval}. Off when {@code
 * kaizent.scheduling.enabled=false} (tests call {@link IntakePoller} directly).
 */
@Component
@ConditionalOnProperty(
    name = "kaizent.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true)
class IntakeSchedule {

  private final IntakePoller poller;

  IntakeSchedule(IntakePoller poller) {
    this.poller = poller;
  }

  @Scheduled(fixedDelayString = "${kaizent.intake.poll-interval:10s}")
  void poll() {
    poller.poll();
  }
}
