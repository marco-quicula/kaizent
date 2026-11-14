package br.com.refatorado.kaizent.llm.adapter.cli;

import br.com.refatorado.kaizent.llm.LlmException;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the single JSON result of {@code claude -p --output-format json --json-schema ...} (not
 * validated yet with a real call; the fields come from the Claude Code headless documentation). The
 * answer is the {@code structured_output} field, serialized as JSON. The {@code result} field (the
 * free text) is never used as the structured answer: when {@code structured_output} is missing, for
 * example because Claude declined the request, the read fails with a clear message that quotes a
 * part of {@code result}. The usage is {@code usage.input_tokens} and {@code usage.output_tokens}
 * when they exist, and unknown otherwise; the field names inside {@code usage} are still to be
 * confirmed by the real validation. {@code total_cost_usd} is ignored: a subscription has no price
 * per call.
 */
@Component
public class ClaudeEvents implements CliEvents {

  public static final String NAME = "claude";

  private static final int EXCERPT_LENGTH = 200;

  private final JsonMapper json = JsonMapper.shared();

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public Optional<String> answer(String stdout) {
    if (stdout == null || stdout.isBlank()) {
      return Optional.empty();
    }
    JsonNode result = parse(stdout);
    if (result == null || !result.isObject()) {
      throw new LlmException(
          "Claude Code's output is not a JSON object: " + excerpt(stdout.strip()));
    }
    JsonNode structured = result.path("structured_output");
    if (structured.isMissingNode() || structured.isNull()) {
      throw new LlmException(
          "Claude Code returned no structured_output (result: "
              + excerpt(result.path("result").asString(""))
              + ")");
    }
    return Optional.of(json.writeValueAsString(structured));
  }

  @Override
  public Tokens tokens(String stdout) {
    JsonNode result = stdout == null || stdout.isBlank() ? null : parse(stdout);
    if (result == null) {
      return Tokens.UNKNOWN;
    }
    JsonNode usage = result.path("usage");
    return new Tokens(count(usage.path("input_tokens")), count(usage.path("output_tokens")));
  }

  private static Integer count(JsonNode tokens) {
    return tokens.isIntegralNumber() ? tokens.asInt() : null;
  }

  private JsonNode parse(String stdout) {
    try {
      return json.readTree(stdout);
    } catch (JacksonException e) {
      return null;
    }
  }

  private static String excerpt(String text) {
    String flat = text.replaceAll("\\s+", " ").strip();
    return flat.length() <= EXCERPT_LENGTH ? flat : flat.substring(0, EXCERPT_LENGTH) + "...";
  }
}
