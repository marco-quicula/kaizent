package br.com.refatorado.kaizent.llm.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmOutputException;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.llm.Usage;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Spec 0004 AC-2: schema, validation and the one-shot repair. */
class StructuredOutputsTest {

  /** A small output type, with the same kinds of constraints as the real ones. */
  record Answer(@NotBlank String text, @DecimalMax("1.0") double confidence) {}

  private static final String VALID = "{\"text\": \"Hello\", \"confidence\": 0.8}";

  private final StructuredOutputs outputs = TestStructuredOutputs.create();

  @Test
  void returnsAValidFirstAnswerWithoutRepair() {
    ScriptedCall call = new ScriptedCall(VALID);

    LlmResponse<Answer> response = outputs.complete(request(), call);

    assertThat(response.output()).isEqualTo(new Answer("Hello", 0.8));
    assertThat(response.rawOutput()).isEqualTo(VALID);
    assertThat(response.usage().inputTokens()).isEqualTo(10);
    assertThat(call.users).hasSize(1);
    assertThat(call.users.getFirst())
        .startsWith("The question")
        .contains("Answer with a single JSON object that matches this JSON schema")
        .contains("\"confidence\"");
  }

  @Test
  void acceptsAnAnswerInsideAMarkdownCodeFence() {
    LlmResponse<Answer> response =
        outputs.complete(request(), new ScriptedCall("```json\n" + VALID + "\n```"));

    assertThat(response.output().text()).isEqualTo("Hello");
  }

  @Test
  void repairsAnInvalidAnswerOnceAndAddsTheUsageOfBothCalls() {
    ScriptedCall call = new ScriptedCall("{\"text\": \"Hello\"}", VALID);

    LlmResponse<Answer> response = outputs.complete(request(), call);

    assertThat(response.output()).isEqualTo(new Answer("Hello", 0.8));
    assertThat(response.usage().inputTokens()).isEqualTo(20);
    assertThat(response.usage().outputTokens()).isEqualTo(10);
    assertThat(call.users).hasSize(2);
    assertThat(call.users.get(1))
        .contains("Your previous answer was not valid:")
        .contains("confidence")
        .contains("Your previous answer was:\n{\"text\": \"Hello\"}");
  }

  @Test
  void failsAfterASecondInvalidAnswer() {
    ScriptedCall call = new ScriptedCall("not json", "{\"text\": \"\", \"confidence\": 0.5}");

    assertThatExceptionOfType(LlmOutputException.class)
        .isThrownBy(() -> outputs.complete(request(), call))
        .withMessageContaining("is not a valid Answer, even after one repair attempt")
        .withMessageContaining("text must not be blank")
        .satisfies(e -> assertThat(e.rawOutput()).contains("\"text\": \"\""));
    assertThat(call.users).hasSize(2);
  }

  @Test
  void rejectsMissingUnknownNullAndOutOfRangeValues() {
    assertThat(outputs.map("{\"text\": \"Hi\"}", Answer.class).error()).contains("confidence");
    assertThat(
            outputs
                .map("{\"text\": \"Hi\", \"confidence\": 0.5, \"extra\": 1}", Answer.class)
                .error())
        .contains("extra");
    assertThat(outputs.map("{\"text\": null, \"confidence\": 0.5}", Answer.class).error())
        .contains("text");
    assertThat(outputs.map("{\"text\": \"Hi\", \"confidence\": 1.5}", Answer.class).error())
        .isEqualTo("confidence must be less than or equal to 1.0");
    assertThat(outputs.map("null", Answer.class).error()).contains("not a JSON object");
    assertThat(outputs.map(" ", Answer.class).error()).isEqualTo("the answer is empty");
  }

  /** A small output type with every scalar kind. */
  record Scalars(String text, int count, double ratio, boolean flag) {}

  @Test
  void rejectsValuesOfTheWrongJsonType() {
    String valid = "{\"text\": \"a\", \"count\": 1, \"ratio\": 0.5, \"flag\": true}";
    assertThat(outputs.map(valid, Scalars.class).value()).isEqualTo(new Scalars("a", 1, 0.5, true));

    for (String invalid :
        List.of(
            "{\"text\": \"a\", \"count\": 1, \"ratio\": \"0.5\", \"flag\": true}",
            "{\"text\": \"a\", \"count\": \"1\", \"ratio\": 0.5, \"flag\": true}",
            "{\"text\": \"a\", \"count\": 1.5, \"ratio\": 0.5, \"flag\": true}",
            "{\"text\": \"a\", \"count\": 1, \"ratio\": 0.5, \"flag\": \"true\"}",
            "{\"text\": 5, \"count\": 1, \"ratio\": 0.5, \"flag\": true}",
            "{\"text\": true, \"count\": 1, \"ratio\": 0.5, \"flag\": true}")) {
      assertThat(outputs.map(invalid, Scalars.class).error()).as(invalid).isNotNull();
    }
  }

  enum Choice {
    A,
    B
  }

  /** Output types whose schema constraints Jackson alone would not enforce. */
  record Selected(Choice value) {}

  record Values(List<String> values) {}

  @Test
  void validatesAgainstTheSchemaWhatMappingAloneWouldAccept() {
    assertThat(outputs.map("{\"value\": \"B\"}", Selected.class).value())
        .isEqualTo(new Selected(Choice.B));
    assertThat(outputs.map("{\"value\": 0}", Selected.class).error()).contains("/value:");
    assertThat(outputs.map("{\"value\": \"C\"}", Selected.class).error()).contains("/value:");
    assertThat(outputs.map("{\"values\": [\"a\"]}", Values.class).value())
        .isEqualTo(new Values(List.of("a")));
    assertThat(outputs.map("{\"values\": [null]}", Values.class).error()).contains("/values/0:");
  }

  @Test
  void repairsAnAnswerThatOnlyTheSchemaRejects() {
    ScriptedCall call = new ScriptedCall("{\"values\": [null]}", "{\"values\": [\"a\"]}");

    LlmResponse<Values> response =
        outputs.complete(
            new LlmRequest<>("step", "The system", "The question", Values.class, Map.of()), call);

    assertThat(response.output()).isEqualTo(new Values(List.of("a")));
    assertThat(call.users).hasSize(2);
    assertThat(call.users.get(1)).contains("/values/0:");
  }

  @Test
  void generatesASchemaThatRequiresEveryPropertyAndForbidsOthers() {
    String schema = outputs.schema(Answer.class);

    assertThat(schema)
        .contains("\"required\"")
        .contains("\"text\"")
        .contains("\"confidence\"")
        .contains("\"additionalProperties\" : false");
  }

  private static LlmRequest<Answer> request() {
    return new LlmRequest<>("step", "The system", "The question", Answer.class, Map.of());
  }

  /** Replies with the given texts in order, each with 10 input and 5 output tokens. */
  private static final class ScriptedCall implements ModelCall {
    private final Deque<String> replies;
    final List<String> users = new ArrayList<>();

    ScriptedCall(String... replies) {
      this.replies = new ArrayDeque<>(List.of(replies));
    }

    @Override
    public Reply call(String system, String user) {
      users.add(user);
      return new Reply(
          replies.removeFirst(), new Usage(10, 5, null, CostType.API, "test", "model"));
    }
  }
}
