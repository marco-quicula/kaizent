package br.com.refatorado.kaizent.llm.adapter.springai;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.llm.adapter.Retrying;
import br.com.refatorado.kaizent.llm.adapter.StructuredOutputs;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.AnthropicSetup;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The {@code anthropic} adapter: the Anthropic API through Spring AI (spec 0004 AC-3). It needs
 * {@code ANTHROPIC_API_KEY}.
 */
@Component
public class AnthropicLlmAdapter extends SpringAiLlmAdapter {

  public static final String NAME = "anthropic";
  private static final Duration TIMEOUT = Duration.ofMinutes(5);

  /** The Anthropic API requires a maximum; a structured answer fits well within it. */
  static final int MAX_TOKENS = 4096;

  private final String apiKey;
  private final String baseUrl;

  public AnthropicLlmAdapter(
      @Value("${kaizent.llm.anthropic.api-key:}") String apiKey,
      @Value("${kaizent.llm.anthropic.base-url:}") String baseUrl,
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
        ? Optional.of("ANTHROPIC_API_KEY is not set: add it to .env to use the anthropic adapter")
        : Optional.empty();
  }

  @Override
  String provider() {
    return "Anthropic API";
  }

  @Override
  ChatModel createModel(String modelId) {
    return AnthropicChatModel.builder()
        .anthropicClient(
            AnthropicSetup.setupSyncClient(
                baseUrl, apiKey, TIMEOUT, 0 /* Retrying retries */, null, Map.of()))
        .options(AnthropicChatOptions.builder().model(modelId).maxTokens(MAX_TOKENS).build())
        .build();
  }

  @Override
  Usage usage(ChatResponse response, String modelId) {
    // output_tokens includes the thinking tokens, which Anthropic bills as output.
    if (nativeUsage(response) instanceof com.anthropic.models.messages.Usage usage) {
      return new Usage(
          Math.toIntExact(usage.inputTokens()),
          Math.toIntExact(usage.outputTokens()),
          null,
          CostType.API,
          NAME,
          modelId);
    }
    return new Usage(null, null, null, CostType.API, NAME, modelId);
  }
}
