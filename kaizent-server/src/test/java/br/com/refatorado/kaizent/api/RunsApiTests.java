package br.com.refatorado.kaizent.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.Database;
import br.com.refatorado.kaizent.Http;
import br.com.refatorado.kaizent.KaizentIntegrationTest;
import br.com.refatorado.kaizent.intake.IncomingTicket;
import br.com.refatorado.kaizent.intake.internal.IntakeService;
import br.com.refatorado.kaizent.run.Run;
import br.com.refatorado.kaizent.run.internal.RunOrchestrator;
import br.com.refatorado.kaizent.workspace.Workspace;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileSystemUtils;
import tools.jackson.databind.JsonNode;

/** Spec 0002 AC-5, AC-6 and AC-7, and the evaluation runs of spec 0003 T6. */
@KaizentIntegrationTest
class RunsApiTests {

  private static final String APPROVE = "{\"type\":\"APPROVED\"}";

  @LocalServerPort private int port;
  @Autowired private IntakeService intake;
  @Autowired private RunOrchestrator orchestrator;
  @Autowired private Workspace workspace;
  @Autowired private JdbcTemplate jdbc;

  private Http http;

  @BeforeEach
  void setUp() {
    Database.clear(jdbc);
    FileSystemUtils.deleteRecursively(workspace.outbox().toFile());
    http = new Http(port);
  }

  @Test
  void listsRunsNewestFirstWithTicketAndStatus() {
    receive("PC-001", "2026-10-01T10:00:00Z");
    receive("PC-002", "2026-10-01T11:00:00Z");
    orchestrator.processNext();

    HttpResponse<String> response = http.get("/api/v1/runs?size=10");

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode page = Http.json(response);
    assertThat(page.at("/totalItems").asLong()).isEqualTo(2);
    assertThat(page.at("/items/0/ticketKey").asString()).isEqualTo("PC-002");
    assertThat(page.at("/items/0/status").asString()).isEqualTo("RECEIVED");
    assertThat(page.at("/items/1/ticketKey").asString()).isEqualTo("PC-001");
    assertThat(page.at("/items/1/status").asString()).isEqualTo("AWAITING_REVIEW");
    assertThat(page.at("/items/1/title").asString()).isEqualTo("Title PC-001");
  }

  @Test
  void leavesEvaluationRunsOutOfTheList() {
    receive("PC-001", "2026-10-01T10:00:00Z");
    receive("PC-002", "2026-10-01T11:00:00Z");
    receive("PC-003", "2026-10-01T12:00:00Z");
    Database.makeEvaluationRuns(jdbc, "PC-002");
    Database.makeEvaluationRuns(jdbc, "PC-003");
    orchestrator.processNext();

    JsonNode page = Http.json(http.get("/api/v1/runs?size=10"));

    assertThat(page.at("/totalItems").asLong()).isEqualTo(1);
    assertThat(page.at("/items").size()).isEqualTo(1);
    assertThat(page.at("/items/0/ticketKey").asString()).isEqualTo("PC-001");
  }

  @Test
  void showsAnEvaluatedRunAndRejectsAVerdictOnIt() {
    receive("PC-001", "2026-10-01T10:00:00Z");
    Database.makeEvaluationRuns(jdbc, "PC-001");
    Run evaluated = orchestrator.processNext().orElseThrow();

    JsonNode detail = Http.json(http.get("/api/v1/runs/" + evaluated.id()));
    HttpResponse<String> verdict =
        http.postJson("/api/v1/runs/" + evaluated.id() + "/verdict", APPROVE);

    assertThat(detail.at("/status").asString()).isEqualTo("EVALUATED");
    assertThat(detail.at("/proposal/suggestedReply").asString()).isNotBlank();
    assertThat(verdict.statusCode()).isEqualTo(409);
    assertThat(workspace.outbox()).doesNotExist();
  }

  @Test
  void returnsTheProblemAndTheProposal() {
    Run run = awaitingReview("PC-001");

    JsonNode detail = Http.json(http.get("/api/v1/runs/" + run.id()));

    assertThat(detail.at("/status").asString()).isEqualTo("AWAITING_REVIEW");
    assertThat(detail.at("/problem/ticketKey").asString()).isEqualTo("PC-001");
    assertThat(detail.at("/problem/body").asString()).isEqualTo("Body of PC-001");
    assertThat(detail.at("/proposal/diagnosis").asString()).startsWith("Fake diagnosis");
    assertThat(detail.at("/proposal/rawConfidence").asDouble()).isEqualTo(0.5);
    assertThat(detail.has("verdict")).isFalse();
  }

  @Test
  void approvingDeliversTheReplyToTheOutbox() throws IOException {
    Run run = awaitingReview("PC-001");

    HttpResponse<String> response = http.postJson("/api/v1/runs/" + run.id() + "/verdict", APPROVE);

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode detail = Http.json(response);
    assertThat(detail.at("/status").asString()).isEqualTo("DELIVERED");
    assertThat(detail.at("/verdict/type").asString()).isEqualTo("APPROVED");
    assertThat(detail.at("/verdict/reviewer").asString()).isEqualTo("local");
    Path reply = workspace.outbox().resolve("PC-001.reply.md");
    assertThat(Files.readString(reply))
        .startsWith("---\nticket: PC-001\nrun: " + run.id() + "\nverdict: APPROVED\n")
        .contains("language: und\n---\n")
        .contains("scripted reply from the fake LLM adapter");
  }

  @Test
  void rejectsAVerdictOnARunThatIsNotAwaitingReview() {
    Run run = awaitingReview("PC-001");
    http.postJson("/api/v1/runs/" + run.id() + "/verdict", APPROVE);

    HttpResponse<String> again = http.postJson("/api/v1/runs/" + run.id() + "/verdict", APPROVE);

    assertThat(again.statusCode()).isEqualTo(409);
    assertThat(again.headers().firstValue("Content-Type")).hasValue("application/problem+json");
    assertThat(Http.json(again).at("/detail").asString()).contains("is DELIVERED");
  }

  @Test
  void rejectsAVerdictOnAFailedRun() {
    receive("PC-ERR", "2026-10-01T10:00:00Z");
    Run failed = orchestrator.processNext().orElseThrow();

    assertThat(http.postJson("/api/v1/runs/" + failed.id() + "/verdict", APPROVE).statusCode())
        .isEqualTo(409);
  }

  @Test
  void rejectsVerdictTypesThatAreNotSupportedYet() {
    Run run = awaitingReview("PC-001");

    assertThat(
            http.postJson("/api/v1/runs/" + run.id() + "/verdict", "{\"type\":\"REJECTED\"}")
                .statusCode())
        .isEqualTo(400);
    assertThat(
            http.postJson("/api/v1/runs/" + run.id() + "/verdict", "{\"type\":\"MAYBE\"}")
                .statusCode())
        .isEqualTo(400);
    assertThat(Http.json(http.get("/api/v1/runs/" + run.id())).at("/status").asString())
        .isEqualTo("AWAITING_REVIEW");
  }

  @Test
  void returnsNotFoundForAnUnknownRun() {
    String unknown = "/api/v1/runs/" + UUID.randomUUID();

    assertThat(http.get(unknown).statusCode()).isEqualTo(404);
    assertThat(http.postJson(unknown + "/verdict", APPROVE).statusCode()).isEqualTo(404);
  }

  private Run awaitingReview(String key) {
    receive(key, "2026-10-01T10:00:00Z");
    return orchestrator.processNext().orElseThrow();
  }

  private void receive(String key, String created) {
    intake.ingest(
        "test",
        new IncomingTicket(
            key,
            "Title " + key,
            "Body of " + key,
            null,
            null,
            Instant.parse(created),
            List.of(),
            "h",
            null));
  }
}
