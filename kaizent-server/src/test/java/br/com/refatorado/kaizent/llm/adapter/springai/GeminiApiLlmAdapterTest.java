package br.com.refatorado.kaizent.llm.adapter.springai;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import br.com.refatorado.kaizent.llm.LlmException;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.adapter.TestStructuredOutputs;
import br.com.refatorado.kaizent.resolution.ResolutionOutput;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** No network calls: CI never calls real models (ADR-0020). */
class GeminiApiLlmAdapterTest {

  private final LlmRequest<ResolutionOutput> request =
      new LlmRequest<>("resolution", "system", "user", ResolutionOutput.class, Map.of());

  @Test
  void failsClearlyWithoutAnApiKey() {
    GeminiApiLlmAdapter adapter = new GeminiApiLlmAdapter("  ", TestStructuredOutputs.create());

    assertThatExceptionOfType(LlmException.class)
        .isThrownBy(() -> adapter.complete(request, new LlmProfile("gemini-api", "gemini-x")))
        .withMessageContaining("GEMINI_API_KEY is not set");
  }

  @Test
  void failsClearlyWithoutAModel() {
    GeminiApiLlmAdapter adapter = new GeminiApiLlmAdapter("key", TestStructuredOutputs.create());

    assertThatExceptionOfType(LlmException.class)
        .isThrownBy(() -> adapter.complete(request, new LlmProfile("gemini-api", " ")))
        .withMessageContaining("No model configured");
  }
}
