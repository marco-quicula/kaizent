package br.com.refatorado.kaizent.llm.adapter.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmAdapter;
import br.com.refatorado.kaizent.llm.LlmCallException;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.llm.adapter.Retrying;
import br.com.refatorado.kaizent.llm.adapter.TestRetrying;
import br.com.refatorado.kaizent.llm.adapter.TestStructuredOutputs;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * The openai, anthropic and ollama adapters against a local stub of each API (spec 0004 AC-3, AC-4
 * and AC-8): the answer and the usage, retries of a 429 with the provider's wait, the provider's
 * cause in the error, and the API key never in errors or logs.
 */
@ExtendWith(OutputCaptureExtension.class)
class ProviderAdaptersTest {

  private static final String KEY = "sk-test-0123456789-secret";
  private static final String ANSWER = "{\\\"category\\\":\\\"bug\\\"}";

  record Triage(@NotBlank String category) {}

  /** One provider: how to build its adapter and what its API answers. */
  record Provider(
      String name,
      String path,
      Function<Setup, LlmAdapter> adapter,
      String success,
      String rateLimited,
      String unauthorized,
      String authHeader,
      String authValue) {

    @Override
    public String toString() {
      return name;
    }
  }

  record Setup(String url, Retrying retrying) {}

  static Stream<Provider> providers() {
    return Stream.of(
        new Provider(
            "openai",
            "/v1/chat/completions",
            s ->
                new OpenAiLlmAdapter(
                    KEY, s.url() + "/v1", TestStructuredOutputs.create(), s.retrying()),
            """
            {"id":"c1","object":"chat.completion","created":1,"model":"m",
             "choices":[{"index":0,"finish_reason":"stop","logprobs":null,
               "message":{"role":"assistant","content":"%s","refusal":null}}],
             "usage":{"prompt_tokens":12,"completion_tokens":7,"total_tokens":19}}
            """
                .formatted(ANSWER),
            """
            {"error":{"message":"Rate limit reached","type":"requests","code":"rate_limit_exceeded"}}
            """,
            """
            {"error":{"message":"Incorrect API key","type":"invalid_request_error","code":"invalid_api_key"}}
            """,
            "Authorization",
            "Bearer " + KEY),
        new Provider(
            "anthropic",
            "/v1/messages",
            s ->
                new AnthropicLlmAdapter(KEY, s.url(), TestStructuredOutputs.create(), s.retrying()),
            """
            {"id":"msg_1","type":"message","role":"assistant","model":"m",
             "content":[{"type":"text","text":"%s"}],
             "stop_reason":"end_turn","stop_sequence":null,
             "usage":{"input_tokens":12,"output_tokens":7}}
            """
                .formatted(ANSWER),
            """
            {"type":"error","error":{"type":"rate_limit_error","message":"Rate limit reached"}}
            """,
            """
            {"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}
            """,
            "x-api-key",
            KEY),
        new Provider(
            "ollama",
            "/api/chat",
            s -> new OllamaLlmAdapter(s.url(), TestStructuredOutputs.create(), s.retrying()),
            """
            {"model":"m","created_at":"2026-11-04T00:00:00Z",
             "message":{"role":"assistant","content":"%s"},
             "done":true,"done_reason":"stop","prompt_eval_count":12,"eval_count":7}
            """
                .formatted(ANSWER),
            """
            {"error":"server busy"}
            """,
            """
            {"error":"unauthorized"}
            """,
            null,
            null));
  }

  private final LlmRequest<Triage> request =
      new LlmRequest<>("triage", "system", "user", Triage.class, Map.of());
  private final List<Duration> waits = TestRetrying.waits();
  private ProviderStub stub;

  @BeforeEach
  void startStub() throws IOException {
    stub = new ProviderStub();
  }

  @AfterEach
  void stopStub() {
    stub.close();
  }

  @ParameterizedTest
  @MethodSource("providers")
  void returnsTheAnswerAndTheProvidersTokenCounts(Provider provider) {
    stub.answer(200, provider.success());

    LlmResponse<Triage> response = complete(provider);

    assertThat(response.output()).isEqualTo(new Triage("bug"));
    assertThat(response.usage())
        .isEqualTo(new Usage(12, 7, null, CostType.API, provider.name(), "m"));
    assertThat(stub.received())
        .singleElement()
        .satisfies(r -> {
          assertThat(r.path()).isEqualTo(provider.path());
          if (provider.authHeader() != null) {
            assertThat(r.headers().get(capitalized(provider.authHeader())))
                .containsExactly(provider.authValue());
          }
        });
  }

  @ParameterizedTest
  @MethodSource("providers")
  void retriesARateLimitWaitingWhatTheProviderAsks(Provider provider) {
    stub.answer(429, Map.of("Retry-After", "5"), provider.rateLimited())
        .answer(200, provider.success());

    LlmResponse<Triage> response = complete(provider);

    assertThat(response.output()).isEqualTo(new Triage("bug"));
    assertThat(waits).containsExactly(Duration.ofSeconds(5));
    assertThat(stub.received()).hasSize(2);
  }

  @ParameterizedTest
  @MethodSource("providers")
  void keepsTheProvidersCauseAndNeverShowsTheKey(Provider provider, CapturedOutput output) {
    stub.answer(429, provider.rateLimited())
        .answer(429, provider.rateLimited())
        .answer(401, provider.unauthorized());

    assertThatExceptionOfType(LlmCallException.class)
        .isThrownBy(() -> complete(provider))
        .satisfies(e -> {
          assertThat(e.status()).isEqualTo(401);
          assertThat(e.retryable()).isFalse();
          assertThat(e.getMessage()).contains("HTTP 401").doesNotContain(KEY);
        });
    assertThat(waits).hasSize(2);
    assertThat(output).contains("Model call failed").contains("HTTP 429").doesNotContain(KEY);
  }

  @Test
  void needsAKeyOrAUrlToBeAvailable() {
    Retrying none = TestRetrying.none();
    assertThat(
            new OpenAiLlmAdapter(" ", "", TestStructuredOutputs.create(), none).unavailableReason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("OPENAI_API_KEY"));
    assertThat(
            new AnthropicLlmAdapter("", "", TestStructuredOutputs.create(), none)
                .unavailableReason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("ANTHROPIC_API_KEY"));
    assertThat(new OllamaLlmAdapter("", TestStructuredOutputs.create(), none).unavailableReason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("OLLAMA_BASE_URL"));
    assertThat(
            new OpenAiLlmAdapter(KEY, "", TestStructuredOutputs.create(), none).unavailableReason())
        .isEmpty();
  }

  private LlmResponse<Triage> complete(Provider provider) {
    LlmAdapter adapter =
        provider.adapter().apply(new Setup(stub.url(), TestRetrying.recording(waits)));
    return adapter.complete(request, new LlmProfile(provider.name(), "m"));
  }

  /** The JDK server normalizes header names to "X-api-key". */
  private static String capitalized(String header) {
    return Character.toUpperCase(header.charAt(0)) + header.substring(1).toLowerCase();
  }
}
