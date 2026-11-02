package br.com.refatorado.kaizent.delivery;

import java.time.Instant;
import java.util.UUID;

/**
 * An approved reply to deliver (ADR-0028).
 *
 * @param ticketKey the ticket the reply answers
 * @param verdict the verdict type, for example {@code APPROVED}
 * @param language the reply language (BCP 47), or {@code und} when unknown
 */
public record Reply(
    String ticketKey,
    UUID runId,
    String verdict,
    Instant approvedAt,
    String language,
    String text) {}
