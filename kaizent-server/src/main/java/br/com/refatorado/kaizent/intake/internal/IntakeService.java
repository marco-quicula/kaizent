package br.com.refatorado.kaizent.intake.internal;

import br.com.refatorado.kaizent.intake.IncomingTicket;
import br.com.refatorado.kaizent.intake.Problem;
import br.com.refatorado.kaizent.intake.ProblemReceived;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores each new ticket once, as a {@link Problem}, and publishes {@link ProblemReceived} in the
 * same transaction. A ticket is new when its {@code (source, externalKey)} has not been seen
 * (ADR-0028), so a ticket never yields a second run, also across restarts.
 */
@Service
public class IntakeService {

  private static final Logger log = LoggerFactory.getLogger(IntakeService.class);

  private final ProblemRepository problems;
  private final JdbcAggregateTemplate aggregates;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  IntakeService(
      ProblemRepository problems,
      JdbcAggregateTemplate aggregates,
      ApplicationEventPublisher events,
      Clock clock) {
    this.problems = problems;
    this.aggregates = aggregates;
    this.events = events;
    this.clock = clock;
  }

  /** Stores the ticket if it is new. Returns the new problem, or empty if it was already seen. */
  @Transactional
  public Optional<Problem> ingest(String source, IncomingTicket ticket) {
    if (problems.existsBySourceAndExternalKey(source, ticket.externalKey())) {
      return Optional.empty();
    }
    Problem problem = insert(source, ticket);
    events.publishEvent(new ProblemReceived(problem.id(), source, problem.externalKey()));
    log.info("Received ticket {} from {}", problem.externalKey(), source);
    return Optional.of(problem);
  }

  /**
   * Stores the ticket without publishing {@link ProblemReceived}, so no review run is created. Used
   * for evaluation runs (spec 0003).
   */
  @Transactional
  public Problem store(String source, IncomingTicket ticket) {
    return insert(source, ticket);
  }

  private Problem insert(String source, IncomingTicket ticket) {
    return aggregates.insert(
        new Problem(
            UUID.randomUUID(),
            source,
            ticket.externalKey(),
            ticket.title(),
            ticket.body(),
            ticket.requester(),
            ticket.channel(),
            ticket.createdAt(),
            clock.instant(),
            ticket.labels(),
            ticket.contentHash(),
            ticket.attributes()));
  }
}
