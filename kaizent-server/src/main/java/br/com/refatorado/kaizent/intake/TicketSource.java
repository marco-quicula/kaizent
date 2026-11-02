package br.com.refatorado.kaizent.intake;

import java.util.List;

/**
 * Inbound port: a channel that tickets come from (ADR-0006, ADR-0014). Implementations only read;
 * they never modify or delete the source tickets.
 */
public interface TicketSource {

  /** The connector id stored as {@link Problem#source()}, for example {@code file-inbox}. */
  String id();

  /** Every ticket currently available in the source. Deduplication is done by intake. */
  List<IncomingTicket> fetch();
}
