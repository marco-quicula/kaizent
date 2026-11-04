package br.com.refatorado.kaizent.llm.adapter.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmException;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.llm.adapter.TestRetrying;
import br.com.refatorado.kaizent.llm.adapter.TestStructuredOutputs;
import br.com.refatorado.kaizent.resolution.ResolutionOutput;
import com.google.genai.types.GenerateContentResponseUsageMetadata;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** No network calls: CI never calls real models (ADR-0020). */
class GeminiApiLlmAdapterTest {

  private final LlmRequest<ResolutionOutput> request =
      new LlmRequest<>("resolution", "system", "user", ResolutionOutput.class, Map.of());

  @Test
  void failsClearlyWithoutAnApiKey() {
    GeminiApiLlmAdapter adapter =
        new GeminiApiLlmAdapter("  ", TestStructuredOutputs.create(), TestRetrying.none());

    assertThatExceptionOfType(LlmException.class)
        .isThrownBy(() -> adapter.complete(request, new LlmProfile("gemini-api", "gemini-x")))
        .withMessageContaining("GEMINI_API_KEY is not set");
  }

  @Test
  void failsClearlyWithoutAModel() {
    GeminiApiLlmAdapter adapter =
        new GeminiApiLlmAdapter("key", TestStructuredOutputs.create(), TestRetrying.none());

    assertThatExceptionOfType(LlmException.class)
        .isThrownBy(() -> adapter.complete(request, new LlmProfile("gemini-api", " ")))
        .withMessageContaining("No model configured");
  }

  @Test
  void billsTheThinkingTokensAsOutput() {
    Usage usage =
        GeminiApiLlmAdapter.fromMetadata(
            GenerateContentResponseUsageMetadata.builder()
                .promptTokenCount(100)
                .candidatesTokenCount(20)
                .thoughtsTokenCount(900)
                .build(),
            "gemini-x");

    assertThat(usage).isEqualTo(new Usage(100, 920, null, CostType.API, "gemini-api", "gemini-x"));
  }

  @Test
  void countsOnlyTheAnswerWhenNoThinkingIsReported() {
    Usage usage =
        GeminiApiLlmAdapter.fromMetadata(
            GenerateContentResponseUsageMetadata.builder()
                .promptTokenCount(100)
                .candidatesTokenCount(20)
                .build(),
            "gemini-x");

    assertThat(usage.outputTokens()).isEqualTo(20);
  }

  @Test
  void keepsZeroCountsThatTheApiReported() {
    Usage usage =
        GeminiApiLlmAdapter.fromMetadata(
            GenerateContentResponseUsageMetadata.builder()
                .promptTokenCount(0)
                .candidatesTokenCount(0)
                .build(),
            "gemini-x");

    assertThat(usage.inputTokens()).isZero();
    assertThat(usage.outputTokens()).isZero();
  }

  @Test
  void leavesCountsThatTheApiDidNotReportUnknown() {
    assertThat(GeminiApiLlmAdapter.fromMetadata(null, "gemini-x"))
        .satisfies(u -> assertThat(u.inputTokens()).isNull())
        .satisfies(u -> assertThat(u.outputTokens()).isNull());
    Usage partial =
        GeminiApiLlmAdapter.fromMetadata(
            GenerateContentResponseUsageMetadata.builder().promptTokenCount(100).build(),
            "gemini-x");
    assertThat(partial.inputTokens()).isEqualTo(100);
    assertThat(partial.outputTokens()).isNull();
  }
}
