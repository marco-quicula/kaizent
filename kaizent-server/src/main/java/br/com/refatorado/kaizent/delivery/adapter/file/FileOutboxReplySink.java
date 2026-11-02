package br.com.refatorado.kaizent.delivery.adapter.file;

import br.com.refatorado.kaizent.delivery.Reply;
import br.com.refatorado.kaizent.delivery.ReplySink;
import br.com.refatorado.kaizent.workspace.Workspace;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Writes {@code outbox/<ticket-id>.reply.md}: front matter ({@code ticket}, {@code run}, {@code
 * verdict}, {@code approvedAt}, {@code language}) followed by the reply text (ADR-0028). The file
 * is written to a temporary file first and then moved into place.
 */
@Component
public class FileOutboxReplySink implements ReplySink {

  /** Ticket ids become file names, so only safe characters are allowed (no path separators). */
  private static final Pattern SAFE_TICKET_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

  private final Workspace workspace;

  public FileOutboxReplySink(Workspace workspace) {
    this.workspace = workspace;
  }

  @Override
  public void send(Reply reply) {
    Path outbox = workspace.outbox();
    Path target = fileFor(outbox, reply.ticketKey());
    try {
      Files.createDirectories(outbox);
      Path temporary = Files.createTempFile(outbox, ".reply-", ".tmp");
      try {
        Files.writeString(temporary, render(reply), StandardCharsets.UTF_8);
        Files.move(
            temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } finally {
        Files.deleteIfExists(temporary);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot write " + target, e);
    }
  }

  static Path fileFor(Path outbox, String ticketKey) {
    if (ticketKey == null
        || !SAFE_TICKET_KEY.matcher(ticketKey).matches()
        || ticketKey.contains("..")) {
      throw new IllegalArgumentException("Ticket id cannot be used as a file name: " + ticketKey);
    }
    return outbox.resolve(ticketKey + ".reply.md");
  }

  static String render(Reply reply) {
    return """
    ---
    ticket: %s
    run: %s
    verdict: %s
    approvedAt: %s
    language: %s
    ---
    %s
    """
        .formatted(
            reply.ticketKey(),
            reply.runId(),
            reply.verdict(),
            reply.approvedAt(),
            reply.language(),
            reply.text().strip());
  }
}
