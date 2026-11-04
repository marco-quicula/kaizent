package br.com.refatorado.kaizent.llm.adapter.springai;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.llm.adapter.Retrying;
import br.com.refatorado.kaizent.llm.adapter.StructuredOutputs;
import java.util.Optional;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;

/**
 * The {@code ollama} adapter: a local or self-hosted Ollama server through Spring AI (spec 0004
 * AC-3). It needs {@code OLLAMA_BASE_URL}, for example {@code http://localhost:11434}. Its calls
 * have no price unless one is configured.
 */
@Component
public class OllamaLlmAdapter extends SpringAiLlmAdapter {

  public static final String NAME = "ollama";

  private final String baseUrl;

  public OllamaLlmAdapter(
      @Value("${kaizent.llm.ollama.base-url:}") String baseUrl,
      StructuredOutputs structuredOutputs,
      Retrying retrying) {
    super(structuredOutputs, retrying);
    this.baseUrl = isBlank(baseUrl) ? null : baseUrl.strip();
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public Optional<String> unavailableReason() {
    return baseUrl == null
        ? Optional.of("OLLAMA_BASE_URL is not set: add it to .env to use the ollama adapter")
        : Optional.empty();
  }

  @Override
  String provider() {
    return "Ollama";
  }

  @Override
  ChatModel createModel(String modelId) {
    return OllamaChatModel.builder()
        .ollamaApi(
            OllamaApi.builder()
                .baseUrl(baseUrl)
                // Spring's handler keeps the HTTP status and body for the error message.
                .responseErrorHandler(new DefaultResponseErrorHandler())
                .build())
        .options(OllamaChatOptions.builder().model(modelId).format("json").build())
        .retryTemplate(NO_RETRY)
        .build();
  }

  @Override
  Usage usage(ChatResponse response, String modelId) {
    // Spring AI's usage turns a missing count into 0; the metadata keeps Ollama's own counts.
    if (response == null || response.getMetadata() == null) {
      return new Usage(null, null, null, CostType.API, NAME, modelId);
    }
    return new Usage(
        count(response.getMetadata().get("prompt-eval-count")),
        count(response.getMetadata().get("eval-count")),
        null,
        CostType.API,
        NAME,
        modelId);
  }

  private static Integer count(Object value) {
    return value instanceof Number number ? number.intValue() : null;
  }
}
