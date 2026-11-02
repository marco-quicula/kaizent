package br.com.refatorado.kaizent;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.FileSystemUtils;
import tools.jackson.databind.JsonNode;

/**
 * Spec 0002 AC-9: the whole cycle with the real schedulers and the fake LLM adapter, without
 * network access. A ticket file becomes a run, the run awaits review, the approval is delivered to
 * the outbox.
 */
@KaizentIntegrationTest
@TestPropertySource(
    properties = {
      "kaizent.scheduling.enabled=true",
      "kaizent.intake.poll-interval=200ms",
      "kaizent.worker.poll-interval=200ms",
      "kaizent.workspace.path=" + EndToEndTests.WORKSPACE
    })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EndToEndTests {

  static final String WORKSPACE = "target/end-to-end-workspace";
  private static final Duration TIMEOUT = Duration.ofSeconds(20);

  @LocalServerPort private int port;
  @Autowired private JdbcTemplate jdbc;

  @BeforeAll
  static void createWorkspace() throws IOException {
    FileSystemUtils.deleteRecursively(Path.of(WORKSPACE));
    Files.createDirectories(Path.of(WORKSPACE, "inbox"));
  }

  @Test
  void aTicketFileBecomesADeliveredReply() throws Exception {
    Database.clear(jdbc);
    Http http = new Http(port);
    Files.writeString(
        Path.of(WORKSPACE, "inbox", "PC-001.md"),
        """
        ---
        id: PC-001
        title: Cannot save an owner with a formatted phone number
        ---
        The telephone field rejects (555) 123-4567.
        """);

    JsonNode run = awaitRun(http, item -> "AWAITING_REVIEW".equals(item.at("/status").asString()));
    String id = run.at("/id").asString();
    assertThat(run.at("/ticketKey").asString()).isEqualTo("PC-001");

    JsonNode detail = Http.json(http.get("/api/v1/runs/" + id));
    assertThat(detail.at("/proposal/suggestedReply").asString()).isNotBlank();

    JsonNode approved =
        Http.json(http.postJson("/api/v1/runs/" + id + "/verdict", "{\"type\":\"APPROVED\"}"));
    assertThat(approved.at("/status").asString()).isEqualTo("DELIVERED");

    Path reply = Path.of(WORKSPACE, "outbox", "PC-001.reply.md");
    assertThat(reply).exists();
    assertThat(Files.readString(reply))
        .startsWith("---\nticket: PC-001\nrun: " + id + "\nverdict: APPROVED\n")
        .contains(detail.at("/proposal/suggestedReply").asString().strip());

    // The ticket is still in the inbox, and later polls never create a second run.
    Thread.sleep(1000);
    assertThat(Http.json(http.get("/api/v1/runs")).at("/totalItems").asLong()).isEqualTo(1);
    assertThat(Path.of(WORKSPACE, "inbox", "PC-001.md")).exists();
  }

  private static JsonNode awaitRun(Http http, Predicate<JsonNode> condition)
      throws InterruptedException {
    Instant deadline = Instant.now().plus(TIMEOUT);
    while (Instant.now().isBefore(deadline)) {
      for (JsonNode item : Http.json(http.get("/api/v1/runs")).at("/items")) {
        if (condition.test(item)) {
          return item;
        }
      }
      Thread.sleep(100);
    }
    throw new AssertionError("No run reached the expected state within " + TIMEOUT);
  }
}
