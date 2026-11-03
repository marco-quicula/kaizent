package br.com.refatorado.kaizent.intake.internal;

import br.com.refatorado.kaizent.intake.EvaluationProblems;
import br.com.refatorado.kaizent.intake.IncomingTicket;
import br.com.refatorado.kaizent.intake.Problem;
import br.com.refatorado.kaizent.intake.TicketSource;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Reads a ticket from the ticket sources and stores it for an evaluation run. */
@Component
class EvaluationIntake implements EvaluationProblems {

  private final List<TicketSource> sources;
  private final IntakeService intake;

  EvaluationIntake(List<TicketSource> sources, IntakeService intake) {
    this.sources = sources;
    this.intake = intake;
  }

  @Override
  public Optional<Problem> store(String source, String ticketKey) {
    for (TicketSource ticketSource : sources) {
      for (IncomingTicket ticket : ticketSource.fetch()) {
        if (ticket.externalKey().equals(ticketKey)) {
          return Optional.of(intake.store(source, ticket));
        }
      }
    }
    return Optional.empty();
  }
}
