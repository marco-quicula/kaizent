package br.com.refatorado.kaizent.intake;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * The canonical problem (ADR-0028), independent of the channel it came from.
 *
 * @param source the connector that produced it, for example {@code file-inbox}
 * @param externalKey the ticket id in that connector; unique per source
 * @param contentHash a hash of the ticket content as received
 */
@Table("problem")
public record Problem(
    @Id UUID id,
    String source,
    String externalKey,
    String title,
    String body,
    String requester,
    String channel,
    Instant createdAt,
    Instant receivedAt,
    List<String> labels,
    String contentHash,
    ProblemAttributes attributes) {

  public Problem {
    labels = labels == null ? List.of() : List.copyOf(labels);
    attributes = attributes == null ? ProblemAttributes.EMPTY : attributes;
  }
}
