package br.com.refatorado.kaizent.intake;

import java.time.Instant;
import java.util.List;

/** A ticket as read from a {@link TicketSource}, before it becomes a {@link Problem}. */
public record IncomingTicket(
    String externalKey,
    String title,
    String body,
    String requester,
    String channel,
    Instant createdAt,
    List<String> labels,
    String contentHash,
    ProblemAttributes attributes) {

  public IncomingTicket {
    labels = labels == null ? List.of() : List.copyOf(labels);
    attributes = attributes == null ? ProblemAttributes.EMPTY : attributes;
  }
}
