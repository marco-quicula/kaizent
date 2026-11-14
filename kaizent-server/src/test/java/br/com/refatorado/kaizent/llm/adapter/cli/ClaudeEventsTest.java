package br.com.refatorado.kaizent.llm.adapter.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.refatorado.kaizent.llm.LlmException;
import org.junit.jupiter.api.Test;

/**
 * The JSON result of claude -p --output-format json --json-schema. Written from the Claude Code
 * documentation (the envelope has the result text, the session id, the usage and the cost, and the
 * structured output in structured_output), not recorded from a real call.
 */
class ClaudeEventsTest {

  private static final String WITH_STRUCTURED_OUTPUT =
      """
      {"type":"result","subtype":"success","is_error":false,"result":"Done.","session_id":"<id>",\
      "total_cost_usd":0.01,"usage":{"input_tokens":120,"output_tokens":18},\
      "structured_output":{"ok":"yes","n":2}}
      """;

  private final ClaudeEvents events = new ClaudeEvents();

  @Test
  void theAnswerIsTheStructuredOutputAsJson() {
    assertThat(events.answer(WITH_STRUCTURED_OUTPUT)).contains("{\"ok\":\"yes\",\"n\":2}");
  }

  @Test
  void theUsageIsReadFromTheUsageField() {
    assertThat(events.tokens(WITH_STRUCTURED_OUTPUT)).isEqualTo(new CliEvents.Tokens(120, 18));
  }

  @Test
  void theUsageIsUnknownWhenTheFieldsAreMissing() {
    String noUsage = "{\"result\":\"x\",\"structured_output\":{\"ok\":\"yes\"}}";
    String otherNames = "{\"usage\":{\"tokens_in\":5},\"structured_output\":{\"ok\":\"yes\"}}";

    assertThat(events.tokens(noUsage)).isEqualTo(CliEvents.Tokens.UNKNOWN);
    assertThat(events.tokens(otherNames)).isEqualTo(CliEvents.Tokens.UNKNOWN);
    assertThat(events.tokens("not json {")).isEqualTo(CliEvents.Tokens.UNKNOWN);
    assertThat(events.tokens("")).isEqualTo(CliEvents.Tokens.UNKNOWN);
    assertThat(events.tokens(null)).isEqualTo(CliEvents.Tokens.UNKNOWN);
  }

  @Test
  void aMissingStructuredOutputFailsAndQuotesTheResultButDoesNotUseIt() {
    String declined =
        "{\"type\":\"result\",\"is_error\":false,\"result\":\"I cannot help with that.\"}";

    assertThatThrownBy(() -> events.answer(declined))
        .isInstanceOf(LlmException.class)
        .hasMessageContaining("no structured_output")
        .hasMessageContaining("I cannot help with that.");
  }

  @Test
  void theQuotedResultIsShortened() {
    String longResult = "{\"result\":\"" + "x".repeat(500) + "\"}";

    assertThatThrownBy(() -> events.answer(longResult))
        .isInstanceOf(LlmException.class)
        .hasMessageContaining("...")
        .satisfies(e -> assertThat(e.getMessage().length()).isLessThan(300));
  }

  @Test
  void anOutputThatIsNotJsonFails() {
    assertThatThrownBy(() -> events.answer("Error: --json-schema is not a valid JSON Schema"))
        .isInstanceOf(LlmException.class)
        .hasMessageContaining("not a JSON object")
        .hasMessageContaining("--json-schema is not a valid JSON Schema");
    assertThatThrownBy(() -> events.answer("[1,2]")).isInstanceOf(LlmException.class);
  }

  @Test
  void anEmptyOutputHasNoAnswer() {
    assertThat(events.answer("")).isEmpty();
    assertThat(events.answer("  \n")).isEmpty();
    assertThat(events.answer(null)).isEmpty();
  }
}
