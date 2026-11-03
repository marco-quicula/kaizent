package br.com.refatorado.kaizent.intake.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the ticket sources every {@code kaizent.intake.poll-interval}. Off when {@code
 * kaizent.scheduling.enabled=false} (tests call {@link IntakePoller} directly) or {@code
 * kaizent.intake.polling=false} (the {@code eval} profile, which reads the tickets itself).
 */
@Component
@ConditionalOnExpression("${kaizent.scheduling.enabled:true} and ${kaizent.intake.polling:true}")
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
