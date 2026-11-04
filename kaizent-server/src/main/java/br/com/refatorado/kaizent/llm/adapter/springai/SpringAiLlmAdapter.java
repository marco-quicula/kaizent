package br.com.refatorado.kaizent.llm.adapter.springai;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmAdapter;
import br.com.refatorado.kaizent.llm.LlmException;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.llm.adapter.ModelCall;
import br.com.refatorado.kaizent.llm.adapter.ProviderErrors;
import br.com.refatorado.kaizent.llm.adapter.Retrying;
import br.com.refatorado.kaizent.llm.adapter.StructuredOutputs;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

/**
 * The common part of the Spring AI adapters (ADR-0016, spec 0004 AC-3 and AC-4): the client is
 * built on the first call for each model, so the application starts without credentials; each raw
 * call is retried by {@link Retrying} (the SDKs' and Spring AI's own retries are turned off); a
 * failure keeps the provider's cause; and the answer goes through {@link StructuredOutputs}.
 */
abstract class SpringAiLlmAdapter implements LlmAdapter {

  /** Spring AI's own retries are off: {@link Retrying} retries, with the provider's cause. */
  static final RetryTemplate NO_RETRY = new RetryTemplate(RetryPolicy.withMaxRetries(0));

  private final StructuredOutputs structuredOutputs;
  private final Retrying retrying;
  private final ConcurrentMap<String, ChatModel> models = new ConcurrentHashMap<>();

  SpringAiLlmAdapter(StructuredOutputs structuredOutputs, Retrying retrying) {
    this.structuredOutputs = structuredOutputs;
    this.retrying = retrying;
  }

  /** The provider's name in messages, for example {@code Gemini API}. */
  abstract String provider();

  /**
   * The configured credentials, removed from every error message and cause (spec 0004 AC-11); none
   * by default.
   */
  List<String> secrets() {
    return List.of();
  }

  /** Builds the chat model for a model id; called once per model. */
  abstract ChatModel createModel(String modelId);

  /** The usage of a response, from the provider's own counts; unknown counts stay null. */
  abstract Usage usage(ChatResponse response, String modelId);

  @Override
  public CostType costType() {
    return CostType.API;
  }

  @Override
  public final <T> LlmResponse<T> complete(LlmRequest<T> request, LlmProfile profile) {
    Optional<String> unavailable = unavailableReason();
    if (unavailable.isPresent()) {
      throw new LlmException(unavailable.get());
    }
    if (profile.model() == null || profile.model().isBlank()) {
      throw new LlmException("No model configured for step " + request.step());
    }
    return structuredOutputs.complete(
        request, (system, user) -> retrying.call(() -> call(system, user, profile.model())));
  }

  private ModelCall.Reply call(String system, String user, String modelId) {
    Prompt prompt = new Prompt(List.of(new SystemMessage(system), new UserMessage(user)));
    ChatResponse response;
    try {
      response = models.computeIfAbsent(modelId, this::createModel).call(prompt);
    } catch (RuntimeException e) {
      throw ProviderErrors.describe(provider(), e, secrets());
    }
    String text =
        response == null || response.getResult() == null
            ? null
            : response.getResult().getOutput().getText();
    return new ModelCall.Reply(text, usage(response, modelId));
  }

  /** The native usage object of a response, if Spring AI kept it. */
  static Object nativeUsage(ChatResponse response) {
    if (response == null
        || response.getMetadata() == null
        || response.getMetadata().getUsage() == null) {
      return null;
    }
    return response.getMetadata().getUsage().getNativeUsage();
  }

  static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
