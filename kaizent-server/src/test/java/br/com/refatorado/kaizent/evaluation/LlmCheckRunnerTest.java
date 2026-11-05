package br.com.refatorado.kaizent.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmCallException;
import br.com.refatorado.kaizent.llm.LlmGateway;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmProfiles;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.workspace.Workspace;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The {@code llm-check} command (spec 0004 T13), with a stub gateway: no model is called. */
class LlmCheckRunnerTest {

  @TempDir Path root;

  private final ByteArrayOutputStream printed = new ByteArrayOutputStream();
  private final List<LlmRequest<?>> requests = new ArrayList<>();
  private final LlmProfiles profiles =
      new LlmProfiles() {
        @Override
        public Map<String, LlmProfile> byStep() {
          return Map.of("resolution", new LlmProfile("cli", null, "codex"));
        }

        @Override
        public CostType costType(LlmProfile profile) {
          return CostType.SUBSCRIPTION;
        }
      };

  @BeforeEach
  void inbox() throws IOException {
    Files.createDirectories(root.resolve("inbox"));
    Files.writeString(root.resolve("inbox/PC-002.md"), "---\nid: PC-002\n---\nList all owners?");
  }

  @Test
  void printsTheTriageShapedAnswerAndTheUsage() {
    LlmGateway gateway =
        answering(
            new TriageCheck(TriageCheck.Category.QUESTION, "owners", "en", "List the owners.", 0.9),
            new Usage(120, 30, new BigDecimal("0.0012"), CostType.API, "openai", "m"));

    int code = runner(gateway, "PC-002").check();

    assertThat(code).isZero();
    assertThat(output())
        .contains("ticket PC-002, step resolution, adapter cli, preset codex")
        .contains("llm-check: OK in")
        .contains("category:   QUESTION")
        .contains("language:   en")
        .contains(
            "usage:      input tokens 120, output tokens 30, cost USD 0.0012, cost type API,"
                + " adapter openai, model m");
    LlmRequest<?> request = requests.getFirst();
    assertThat(request.step()).isEqualTo("resolution");
    assertThat(request.outputType()).isEqualTo(TriageCheck.class);
    assertThat(request.user()).contains("Ticket PC-002").contains("List all owners?");
    assertThat(request.attributes()).containsEntry(LlmRequest.TICKET_KEY, "PC-002");
  }

  @Test
  void printsUnknownCountsAndTheProvidersCauseOnFailure() {
    LlmGateway failing =
        new LlmGateway() {
          @Override
          public <T> LlmResponse<T> complete(LlmRequest<T> request) {
            throw new LlmCallException(
                "Gemini API", 429, "RESOURCE_EXHAUSTED", "Quota exceeded", null, true, null);
          }
        };

    int code = runner(failing, "PC-002").check();

    assertThat(code).isEqualTo(1);
    assertThat(output())
        .contains("llm-check: FAILED after")
        .contains("HTTP 429 RESOURCE_EXHAUSTED: Quota exceeded");
  }

  @Test
  void needsAnExistingTicketAndAProfile() {
    LlmGateway unused = answering(null, null);

    assertThat(runner(unused, null).check()).isEqualTo(1);
    assertThat(runner(unused, "../secret").check()).isEqualTo(1);
    assertThat(runner(unused, "PC-404").check()).isEqualTo(1);
    assertThat(
            new LlmCheckRunner(
                    unused,
                    profiles,
                    new Workspace(root),
                    new LlmCheckProperties("PC-002", "triage", false),
                    clock(),
                    null,
                    new PrintStream(printed, true, StandardCharsets.UTF_8))
                .check())
        .isEqualTo(1);
    assertThat(output())
        .contains("set --kaizent.llm-check.ticket")
        .contains("ticket PC-404 not found")
        .contains("no profile for step triage");
    assertThat(requests).isEmpty();
  }

  private LlmGateway answering(TriageCheck answer, Usage usage) {
    return new LlmGateway() {
      @Override
      @SuppressWarnings("unchecked")
      public <T> LlmResponse<T> complete(LlmRequest<T> request) {
        requests.add(request);
        return new LlmResponse<>((T) answer, usage, "{}");
      }
    };
  }

  private LlmCheckRunner runner(LlmGateway gateway, String ticket) {
    return new LlmCheckRunner(
        gateway,
        profiles,
        new Workspace(root),
        new LlmCheckProperties(ticket, "resolution", false),
        clock(),
        null,
        new PrintStream(printed, true, StandardCharsets.UTF_8));
  }

  private static Clock clock() {
    return Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);
  }

  private String output() {
    return printed.toString(StandardCharsets.UTF_8);
  }
}
