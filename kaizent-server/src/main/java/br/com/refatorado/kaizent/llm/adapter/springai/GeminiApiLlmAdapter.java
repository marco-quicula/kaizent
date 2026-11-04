package br.com.refatorado.kaizent.llm.adapter.springai;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.llm.adapter.Retrying;
import br.com.refatorado.kaizent.llm.adapter.StructuredOutputs;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponseUsageMetadata;
import java.util.Optional;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The {@code gemini-api} adapter: the Gemini API through Spring AI (ADR-0016). Without {@code
 * GEMINI_API_KEY} the application still starts, and each call fails with a clear message.
 */
@Component
public class GeminiApiLlmAdapter extends SpringAiLlmAdapter {

  public static final String NAME = "gemini-api";

  private final String apiKey;

  public GeminiApiLlmAdapter(
      @Value("${spring.ai.google.genai.api-key:}") String apiKey,
      StructuredOutputs structuredOutputs,
      Retrying retrying) {
    super(structuredOutputs, retrying);
    this.apiKey = apiKey == null ? "" : apiKey.strip();
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public Optional<String> unavailableReason() {
    return isBlank(apiKey)
        ? Optional.of("GEMINI_API_KEY is not set: add it to .env to use the gemini-api adapter")
        : Optional.empty();
  }

  @Override
  String provider() {
    return "Gemini API";
  }

  @Override
  ChatModel createModel(String modelId) {
    return GoogleGenAiChatModel.builder()
        .genAiClient(Client.builder().apiKey(apiKey).build())
        .options(
            GoogleGenAiChatOptions.builder()
                .model(modelId)
                .responseMimeType("application/json")
                .build())
        .retryTemplate(NO_RETRY)
        .build();
  }

  @Override
  Usage usage(ChatResponse response, String modelId) {
    return fromMetadata(
        nativeUsage(response) instanceof GenerateContentResponseUsageMetadata metadata
            ? metadata
            : null,
        modelId);
  }

  /**
   * The usage from the Gemini API's own metadata, not from Spring AI's normalized counts, which
   * turn missing counts into zeros. The billed output includes the thinking tokens, which Gemini
   * reports apart from the answer's tokens. A count the API did not report stays unknown.
   */
  static Usage fromMetadata(GenerateContentResponseUsageMetadata metadata, String model) {
    Integer input = null;
    Integer output = null;
    if (metadata != null) {
      input = metadata.promptTokenCount().orElse(null);
      output =
          metadata
              .candidatesTokenCount()
              .map(candidates -> candidates + metadata.thoughtsTokenCount().orElse(0))
              .orElse(null);
    }
    return new Usage(input, output, null, CostType.API, NAME, model);
  }
}
