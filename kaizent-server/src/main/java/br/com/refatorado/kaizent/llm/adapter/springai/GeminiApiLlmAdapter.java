package br.com.refatorado.kaizent.llm.adapter.springai;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmAdapter;
import br.com.refatorado.kaizent.llm.LlmException;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.llm.adapter.ModelCall;
import br.com.refatorado.kaizent.llm.adapter.StructuredOutputs;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponseUsageMetadata;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The {@code gemini-api} adapter: the Gemini API through Spring AI (ADR-0016). The client is built
 * on the first call, so the application starts without a key; without {@code GEMINI_API_KEY}, each
 * call fails with a clear message. Structured output and its repair are shared with the other
 * adapters ({@link StructuredOutputs}). Spec 0004 adds the other providers and retries.
 */
@Component
public class GeminiApiLlmAdapter implements LlmAdapter {

  public static final String NAME = "gemini-api";

  private final String apiKey;
  private final StructuredOutputs structuredOutputs;
  private final ConcurrentMap<String, GoogleGenAiChatModel> models = new ConcurrentHashMap<>();

  public GeminiApiLlmAdapter(
      @Value("${spring.ai.google.genai.api-key:}") String apiKey,
      StructuredOutputs structuredOutputs) {
    this.apiKey = apiKey == null ? "" : apiKey.strip();
    this.structuredOutputs = structuredOutputs;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public CostType costType() {
    return CostType.API;
  }

  @Override
  public <T> LlmResponse<T> complete(LlmRequest<T> request, LlmProfile profile) {
    if (apiKey.isEmpty()) {
      throw new LlmException(
          "GEMINI_API_KEY is not set: add it to .env to use the gemini-api adapter");
    }
    if (profile.model() == null || profile.model().isBlank()) {
      throw new LlmException("No model configured for step " + request.step());
    }
    return structuredOutputs.complete(request, (system, user) -> call(system, user, profile));
  }

  private ModelCall.Reply call(String system, String user, LlmProfile profile) {
    Prompt prompt = new Prompt(List.of(new SystemMessage(system), new UserMessage(user)));
    ChatResponse response;
    try {
      response = model(profile.model()).call(prompt);
    } catch (RuntimeException e) {
      throw new LlmException("Gemini API call failed: " + e.getMessage(), e);
    }
    String text = response.getResult() == null ? null : response.getResult().getOutput().getText();
    return new ModelCall.Reply(text, usage(response, profile));
  }

  private static Usage usage(ChatResponse response, LlmProfile profile) {
    org.springframework.ai.chat.metadata.Usage reported =
        response.getMetadata() == null ? null : response.getMetadata().getUsage();
    Object nativeUsage = reported == null ? null : reported.getNativeUsage();
    return usage(
        nativeUsage instanceof GenerateContentResponseUsageMetadata metadata ? metadata : null,
        profile.model());
  }

  /**
   * The usage from the Gemini API's own metadata, not from Spring AI's normalized counts, which
   * turn missing counts into zeros. The billed output includes the thinking tokens, which Gemini
   * reports apart from the answer's tokens. A count the API did not report stays unknown.
   */
  static Usage usage(GenerateContentResponseUsageMetadata metadata, String model) {
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

  private GoogleGenAiChatModel model(String modelId) {
    return models.computeIfAbsent(
        modelId,
        id ->
            GoogleGenAiChatModel.builder()
                .genAiClient(Client.builder().apiKey(apiKey).build())
                .options(
                    GoogleGenAiChatOptions.builder()
                        .model(id)
                        .responseMimeType("application/json")
                        .build())
                .build());
  }
}
