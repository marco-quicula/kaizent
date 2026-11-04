package br.com.refatorado.kaizent.llm.adapter;

import br.com.refatorado.kaizent.llm.LlmOutputException;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.llm.Usage;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Structured output for every adapter (spec 0004 AC-2): asks the model for JSON that matches the
 * schema generated from the requested record, maps and validates the answer, and on invalid output
 * makes one repair attempt that sends the validation error back. A second invalid answer fails with
 * {@link LlmOutputException}.
 *
 * <p>An answer is valid when it is a JSON object, possibly inside a Markdown code fence, with every
 * property of the record, of the right type and with no unknown property, and when the record's
 * Bean Validation constraints hold.
 */
@Component
public class StructuredOutputs {

  private final JsonMapper strict;
  private final Validator validator;
  private final SchemaGenerator schemas;
  private final Map<Class<?>, String> schemaCache = new ConcurrentHashMap<>();

  public StructuredOutputs(JsonMapper json, Validator validator) {
    this.strict =
        json.rebuild()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();
    this.validator = validator;
    SchemaGeneratorConfigBuilder config =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
            .with(new JacksonModule())
            .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT);
    config.forFields().withRequiredCheck(field -> true);
    this.schemas = new SchemaGenerator(config.build());
  }

  /** The JSON schema of a record, as text. */
  public String schema(Class<?> type) {
    return schemaCache.computeIfAbsent(type, key -> schemas.generateSchema(key).toPrettyString());
  }

  /** The instructions appended to the user prompt: answer with JSON matching the schema. */
  public String instructions(Class<?> type) {
    return """
    Answer with a single JSON object that matches this JSON schema, and nothing else: no \
    Markdown, no explanation.

    """
        + schema(type);
  }

  /**
   * Runs a structured call: the first answer, and one repair attempt when it is invalid.
   *
   * @throws LlmOutputException if the repaired answer is still invalid
   */
  public <T> LlmResponse<T> complete(LlmRequest<T> request, ModelCall call) {
    String user = request.user() + "\n\n" + instructions(request.outputType());
    ModelCall.Reply first = call.call(request.system(), user);
    Mapping<T> mapped = map(first.text(), request.outputType());
    if (mapped.value() != null) {
      return new LlmResponse<>(mapped.value(), first.usage(), first.text());
    }
    String repair =
        user
            + "\n\nYour previous answer was not valid: "
            + mapped.error()
            + "\n\nYour previous answer was:\n"
            + first.text()
            + "\n\nAnswer again with only the corrected JSON object.";
    ModelCall.Reply second = call.call(request.system(), repair);
    Usage usage = first.usage().plus(second.usage());
    Mapping<T> repaired = map(second.text(), request.outputType());
    if (repaired.value() != null) {
      return new LlmResponse<>(repaired.value(), usage, second.text());
    }
    throw new LlmOutputException(
        "The model's output is not a valid "
            + request.outputType().getSimpleName()
            + ", even after one repair attempt: "
            + repaired.error(),
        second.text());
  }

  /** Maps and validates a text answer. */
  <T> Mapping<T> map(String text, Class<T> type) {
    if (text == null || text.isBlank()) {
      return Mapping.invalid("the answer is empty");
    }
    T value;
    try {
      value = strict.readValue(withoutCodeFence(text.strip()), type);
    } catch (JacksonException e) {
      return Mapping.invalid(e.getOriginalMessage());
    }
    if (value == null) {
      return Mapping.invalid("the answer is null, not a JSON object");
    }
    Set<ConstraintViolation<T>> violations = validator.validate(value);
    if (!violations.isEmpty()) {
      return Mapping.invalid(
          violations.stream()
              .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
              .map(v -> v.getPropertyPath() + " " + v.getMessage())
              .collect(Collectors.joining("; ")));
    }
    return new Mapping<>(value, null);
  }

  private static String withoutCodeFence(String text) {
    if (!text.startsWith("```")) {
      return text;
    }
    List<String> lines = text.lines().toList();
    int end = lines.getLast().strip().equals("```") ? lines.size() - 1 : lines.size();
    return String.join("\n", lines.subList(1, end));
  }

  /** A mapped value, or why the text could not be mapped. */
  record Mapping<T>(T value, String error) {
    static <T> Mapping<T> invalid(String error) {
      return new Mapping<>(null, error);
    }
  }
}
