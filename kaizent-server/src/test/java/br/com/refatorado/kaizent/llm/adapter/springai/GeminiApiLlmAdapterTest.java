package br.com.refatorado.kaizent.llm.adapter.springai;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import br.com.refatorado.kaizent.llm.LlmException;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.resolution.Proposal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** No network calls: CI never calls real models (ADR-0020). */
class GeminiApiLlmAdapterTest {

  private final LlmRequest<Proposal> request =
      new LlmRequest<>("resolution", "system", "user", Proposal.class, Map.of());

  @Test
  void failsClearlyWithoutAnApiKey() {
    GeminiApiLlmAdapter adapter = new GeminiApiLlmAdapter("  ", JsonMapper.shared());

    assertThatExceptionOfType(LlmException.class)
        .isThrownBy(() -> adapter.complete(request, new LlmProfile("gemini-api", "gemini-x")))
        .withMessageContaining("GEMINI_API_KEY is not set");
  }

  @Test
  void failsClearlyWithoutAModel() {
    GeminiApiLlmAdapter adapter = new GeminiApiLlmAdapter("key", JsonMapper.shared());

    assertThatExceptionOfType(LlmException.class)
        .isThrownBy(() -> adapter.complete(request, new LlmProfile("gemini-api", " ")))
        .withMessageContaining("No model configured");
  }
}
