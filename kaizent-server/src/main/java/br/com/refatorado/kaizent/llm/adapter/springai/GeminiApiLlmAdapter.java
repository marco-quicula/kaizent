package br.com.refatorado.kaizent.llm.adapter.springai;

import br.com.refatorado.kaizent.llm.LlmAdapter;
import br.com.refatorado.kaizent.llm.LlmException;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import com.google.genai.Client;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * The {@code gemini-api} adapter: the Gemini API through Spring AI (ADR-0016). The client is built
 * on the first call, so the application starts without a key; without {@code GEMINI_API_KEY}, each
 * call fails with a clear message. Spec 0004 adds the other providers, retries and usage.
 */
@Component
public class GeminiApiLlmAdapter implements LlmAdapter {

  public static final String NAME = "gemini-api";

  private final String apiKey;
  private final JsonMapper json;
  private final ConcurrentMap<String, GoogleGenAiChatModel> models = new ConcurrentHashMap<>();

  public GeminiApiLlmAdapter(
      @Value("${spring.ai.google.genai.api-key:}") String apiKey, JsonMapper json) {
    this.apiKey = apiKey == null ? "" : apiKey.strip();
    this.json = json;
  }

  @Override
  public String name() {
    return NAME;
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
    BeanOutputConverter<T> converter = new BeanOutputConverter<>(request.outputType(), json);
    Prompt prompt =
        new Prompt(
            List.of(
                new SystemMessage(request.system()),
                new UserMessage(request.user() + "\n\n" + converter.getFormat())));
    String text;
    try {
      ChatResponse response = model(profile.model()).call(prompt);
      text = response.getResult().getOutput().getText();
    } catch (RuntimeException e) {
      throw new LlmException("Gemini API call failed: " + e.getMessage(), e);
    }
    if (text == null || text.isBlank()) {
      throw new LlmException("Gemini API returned an empty response");
    }
    try {
      return new LlmResponse<>(converter.convert(text), text);
    } catch (RuntimeException e) {
      throw new LlmException(
          "Gemini API output is not a valid " + request.outputType().getSimpleName(), e);
    }
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
