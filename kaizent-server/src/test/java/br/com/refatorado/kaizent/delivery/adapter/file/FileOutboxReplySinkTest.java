package br.com.refatorado.kaizent.delivery.adapter.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import br.com.refatorado.kaizent.delivery.Reply;
import br.com.refatorado.kaizent.workspace.Workspace;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FileOutboxReplySinkTest {

  private static final UUID RUN = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @TempDir Path root;

  @Test
  void writesTheReplyWithFrontMatterAndCreatesTheOutbox() throws IOException {
    new FileOutboxReplySink(new Workspace(root))
        .send(
            new Reply(
                "PC-001",
                RUN,
                "APPROVED",
                Instant.parse("2026-11-02T10:00:00Z"),
                "und",
                "Hello Ana,\n\nUse ten digits.\n"));

    assertThat(Files.readString(root.resolve("outbox/PC-001.reply.md")))
        .isEqualTo(
            """
            ---
            ticket: PC-001
            run: 00000000-0000-0000-0000-000000000001
            verdict: APPROVED
            approvedAt: 2026-11-02T10:00:00Z
            language: und
            ---
            Hello Ana,

            Use ten digits.
            """);
    try (var files = Files.list(root.resolve("outbox"))) {
      assertThat(files).hasSize(1);
    }
  }

  @Test
  void replacesAPreviousReply() throws IOException {
    FileOutboxReplySink sink = new FileOutboxReplySink(new Workspace(root));
    sink.send(new Reply("PC-001", RUN, "APPROVED", Instant.EPOCH, "und", "First"));
    sink.send(new Reply("PC-001", RUN, "APPROVED", Instant.EPOCH, "und", "Second"));

    assertThat(Files.readString(root.resolve("outbox/PC-001.reply.md"))).endsWith("Second\n");
  }

  @ParameterizedTest
  @ValueSource(strings = {"../escape", "a/b", "a\\b", "..", ".hidden", "", "x..y"})
  void rejectsTicketIdsThatAreNotSafeFileNames(String ticketKey) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FileOutboxReplySink.fileFor(root, ticketKey));
  }
}
