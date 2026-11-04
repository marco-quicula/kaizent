package br.com.refatorado.kaizent.llm.adapter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The events of codex exec --json, as recorded with Codex CLI 0.166.0 (thread id removed). */
class CodexEventsTest {

  private static final String RUN =
      """
      {"type":"thread.started","thread_id":"<id>"}
      {"type":"turn.started"}
      {"type":"item.completed","item":{"id":"item_0","type":"agent_message","text":"{\\"ok\\":true,\\"assunto\\":\\"teste sintetico\\"}"}}
      {"type":"turn.completed","usage":{"input_tokens":14187,"cached_input_tokens":0,"cache_write_input_tokens":0,"output_tokens":22,"reasoning_output_tokens":0}}
      """;

  private final CodexEvents events = new CodexEvents();

  @Test
  void readsTheLastAgentMessage() {
    assertThat(events.answer(RUN)).contains("{\"ok\":true,\"assunto\":\"teste sintetico\"}");
  }

  @Test
  void readsTheTokensOfTheTurns() {
    assertThat(events.tokens(RUN)).isEqualTo(new CliEvents.Tokens(14187, 22));
    String twoTurns =
        RUN
            + "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":100,\"output_tokens\":5}}\n";
    assertThat(events.tokens(twoTurns)).isEqualTo(new CliEvents.Tokens(14287, 27));
  }

  @Test
  void leavesWhatIsMissingUnknownAndSkipsOtherLines() {
    String noUsage = "Reading prompt from stdin...\n{\"type\":\"turn.started\"}\nnot json {\n";

    assertThat(events.answer(noUsage)).isEmpty();
    assertThat(events.tokens(noUsage)).isEqualTo(CliEvents.Tokens.UNKNOWN);
    assertThat(events.tokens(null)).isEqualTo(CliEvents.Tokens.UNKNOWN);
  }
}
