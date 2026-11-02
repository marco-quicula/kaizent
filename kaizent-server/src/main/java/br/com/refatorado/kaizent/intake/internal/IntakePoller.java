package br.com.refatorado.kaizent.intake.internal;

import br.com.refatorado.kaizent.intake.IncomingTicket;
import br.com.refatorado.kaizent.intake.TicketSource;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls every {@link TicketSource} every {@code kaizent.intake.poll-interval}. */
@Component
public class IntakePoller {

  private static final Logger log = LoggerFactory.getLogger(IntakePoller.class);

  private final List<TicketSource> sources;
  private final IntakeService intake;

  IntakePoller(List<TicketSource> sources, IntakeService intake) {
    this.sources = sources;
    this.intake = intake;
  }

  /** Reads every source once and stores the new tickets. Returns how many were new. */
  @Scheduled(fixedDelayString = "${kaizent.intake.poll-interval:10s}")
  public int poll() {
    int received = 0;
    for (TicketSource source : sources) {
      for (IncomingTicket ticket : source.fetch()) {
        try {
          if (intake.ingest(source.id(), ticket).isPresent()) {
            received++;
          }
        } catch (DuplicateKeyException e) {
          log.debug("Ticket {} was stored concurrently", ticket.externalKey());
        }
      }
    }
    return received;
  }
}
