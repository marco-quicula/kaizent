package br.com.refatorado.kaizent.llm.adapter.springai;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.llm.adapter.Retrying;
import br.com.refatorado.kaizent.llm.adapter.StructuredOutputs;
import com.openai.models.completions.CompletionUsage;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.setup.OpenAiSetup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The {@code openai} adapter: the OpenAI API through Spring AI (spec 0004 AC-3). It needs {@code
 * OPENAI_API_KEY}; {@code OPENAI_BASE_URL} points it to another OpenAI-compatible endpoint.
 */
@Component
public class OpenAiLlmAdapter extends SpringAiLlmAdapter {

  public static final String NAME = "openai";
  private static final Duration TIMEOUT = Duration.ofMinutes(5);

  private final String apiKey;
  private final String baseUrl;

  public OpenAiLlmAdapter(
      @Value("${kaizent.llm.openai.api-key:}") String apiKey,
      @Value("${kaizent.llm.openai.base-url:}") String baseUrl,
      StructuredOutputs structuredOutputs,
      Retrying retrying) {
    super(structuredOutputs, retrying);
    this.apiKey = apiKey == null ? "" : apiKey.strip();
    this.baseUrl = isBlank(baseUrl) ? null : baseUrl.strip();
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public Optional<String> unavailableReason() {
    return isBlank(apiKey)
        ? Optional.of("OPENAI_API_KEY is not set: add it to .env to use the openai adapter")
        : Optional.empty();
  }

  @Override
  String provider() {
    return "OpenAI API";
  }

  @Override
  ChatModel createModel(String modelId) {
    // Spring AI's builder also makes an async client; both get the key and no SDK retries.
    return OpenAiChatModel.builder()
        .openAiClient(
            OpenAiSetup.setupSyncClient(
                baseUrl,
                apiKey,
                null,
                null,
                null,
                null,
                false,
                false,
                modelId,
                TIMEOUT,
                0,
                null,
                Map.of(),
                ObservationRegistry.NOOP,
                null,
                List.of()))
        .openAiClientAsync(
            OpenAiSetup.setupAsyncClient(
                baseUrl,
                apiKey,
                null,
                null,
                null,
                null,
                false,
                false,
                modelId,
                TIMEOUT,
                0,
                null,
                Map.of(),
                ObservationRegistry.NOOP,
                null,
                List.of()))
        .options(OpenAiChatOptions.builder().model(modelId).build())
        .build();
  }

  @Override
  Usage usage(ChatResponse response, String modelId) {
    // completion_tokens includes the reasoning tokens, which OpenAI bills as output.
    if (nativeUsage(response) instanceof CompletionUsage usage) {
      return new Usage(
          Math.toIntExact(usage.promptTokens()),
          Math.toIntExact(usage.completionTokens()),
          null,
          CostType.API,
          NAME,
          modelId);
    }
    return new Usage(null, null, null, CostType.API, NAME, modelId);
  }
}
