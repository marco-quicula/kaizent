package br.com.refatorado.kaizent.intake.adapter.file;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.intake.IncomingTicket;
import br.com.refatorado.kaizent.workspace.Workspace;
import br.com.refatorado.kaizent.workspace.WorkspaceLoader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileInboxTicketSourceTest {

  @TempDir Path root;
  private FileInboxTicketSource source;

  @BeforeEach
  void setUp() throws IOException {
    Files.createDirectories(root.resolve("inbox"));
    source = new FileInboxTicketSource(new Workspace(root));
  }

  @Test
  void readsTheFrontMatterAndTheBody() throws IOException {
    write(
        "ticket.md",
        """
        ---
        id: PC-001
        title: Phone format
        requester: Ana
        channel: email
        created: 2026-10-01T10:00:00Z
        labels: [owners, forms]
        ---
        The telephone field rejects my number.
        """);

    IncomingTicket ticket = only(source.fetch());

    assertThat(ticket.externalKey()).isEqualTo("PC-001");
    assertThat(ticket.title()).isEqualTo("Phone format");
    assertThat(ticket.body()).isEqualTo("The telephone field rejects my number.");
    assertThat(ticket.requester()).isEqualTo("Ana");
    assertThat(ticket.channel()).isEqualTo("email");
    assertThat(ticket.createdAt()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
    assertThat(ticket.labels()).containsExactly("owners", "forms");
    assertThat(ticket.contentHash()).hasSize(64);
    assertThat(ticket.attributes().values()).containsEntry("file", "ticket.md");
  }

  @Test
  void appliesTheDefaultsWithoutFrontMatter() throws IOException {
    Path file = write("PC-002.md", "\n# Vets page is empty\n\nNo vets are listed.\n");
    Instant modified = Instant.parse("2026-09-30T08:00:00Z");
    Files.setLastModifiedTime(file, FileTime.from(modified));

    IncomingTicket ticket = only(source.fetch());

    assertThat(ticket.externalKey()).isEqualTo("PC-002");
    assertThat(ticket.title()).isEqualTo("Vets page is empty");
    assertThat(ticket.createdAt()).isEqualTo(modified);
    assertThat(ticket.labels()).isEmpty();
    assertThat(ticket.requester()).isNull();
  }

  @Test
  void ignoresOtherFilesAndSkipsUnreadableTickets() throws IOException {
    write("notes.txt", "not a ticket");
    write("broken.md", "---\nid: [unclosed\n---\nBody\n");
    write("ok.md", "Body");

    assertThat(source.fetch()).extracting(IncomingTicket::externalKey).containsExactly("ok");
  }

  @Test
  void readsTheSameTicketIdsAsTheWorkspaceLoader() throws IOException {
    Files.write(
        root.resolve("inbox").resolve("filename.md"),
        "\uFEFF---\r\nid: T-1\r\n---\r\nInvoice question\r\n".getBytes(StandardCharsets.UTF_8));
    write("plain.md", "No front matter.");
    write("blank-id.md", "---\nid: \" \"\n---\nBody");
    write("unclosed.md", "---\nid: T-4\n---not-a-delimiter\nBody");

    List<String> received = source.fetch().stream().map(IncomingTicket::externalKey).toList();

    assertThat(received).containsExactlyInAnyOrder("T-1", "plain", "blank-id");
    assertThat(WorkspaceLoader.load(new Workspace(root)).ticketIds())
        .containsExactlyInAnyOrderElementsOf(received);
  }

  @Test
  void contentHashChangesWithTheContent() throws IOException {
    write("a.md", "First version");
    String first = only(source.fetch()).contentHash();
    write("a.md", "Second version");

    assertThat(only(source.fetch()).contentHash()).isNotEqualTo(first);
  }

  @Test
  void returnsNothingWhenTheInboxIsMissing() {
    assertThat(new FileInboxTicketSource(new Workspace(root.resolve("missing"))).fetch()).isEmpty();
  }

  private Path write(String name, String content) throws IOException {
    return Files.writeString(root.resolve("inbox").resolve(name), content);
  }

  private static IncomingTicket only(List<IncomingTicket> tickets) {
    assertThat(tickets).hasSize(1);
    return tickets.getFirst();
  }
}
