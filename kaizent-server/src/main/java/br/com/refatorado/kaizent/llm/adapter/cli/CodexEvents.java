package br.com.refatorado.kaizent.llm.adapter.cli;

import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the JSON events of {@code codex exec --json} (validated with Codex CLI 0.166.0): the final
 * answer is the text of the last {@code item.completed} event whose item is an {@code
 * agent_message}, and the usage is the sum of the {@code turn.completed} events' {@code usage}
 * ({@code input_tokens}, {@code output_tokens}). Lines that are not JSON are skipped.
 */
@Component
public class CodexEvents implements CliEvents {

  public static final String NAME = "codex";

  private final JsonMapper json = JsonMapper.shared();

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public Optional<String> answer(String stdout) {
    String answer = null;
    for (JsonNode event : events(stdout)) {
      JsonNode item = event.path("item");
      if ("item.completed".equals(event.path("type").asString(""))
          && "agent_message".equals(item.path("type").asString(""))
          && item.path("text").isString()) {
        answer = item.path("text").asString();
      }
    }
    return Optional.ofNullable(answer);
  }

  @Override
  public Tokens tokens(String stdout) {
    Integer input = null;
    Integer output = null;
    for (JsonNode event : events(stdout)) {
      JsonNode usage = event.path("usage");
      if ("turn.completed".equals(event.path("type").asString("")) && usage.isObject()) {
        input = add(input, usage.path("input_tokens"));
        output = add(output, usage.path("output_tokens"));
      }
    }
    return new Tokens(input, output);
  }

  private static Integer add(Integer sum, JsonNode count) {
    if (!count.isIntegralNumber()) {
      return sum;
    }
    return (sum == null ? 0 : sum) + count.asInt();
  }

  private Iterable<JsonNode> events(String stdout) {
    if (stdout == null) {
      return java.util.List.of();
    }
    return stdout
        .lines()
        .filter(line -> line.startsWith("{"))
        .map(this::parse)
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  private JsonNode parse(String line) {
    try {
      return json.readTree(line);
    } catch (JacksonException e) {
      return null;
    }
  }
}
