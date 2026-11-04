package br.com.refatorado.kaizent.llm.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.llm.LlmCallException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/** Spec 0004 AC-4: a failed call keeps the provider's cause. */
class ProviderErrorsTest {

  @Test
  void keepsTheGeminiStatusCodeMessageAndRetryDelay() {
    String message =
        "You exceeded your current quota. Please retry in 41.2s. {\"retryDelay\": \"41s\"}";
    RuntimeException wrapped =
        new RuntimeException(
            "Failed to generate content",
            new com.google.genai.errors.ApiException(429, "RESOURCE_EXHAUSTED", message));

    LlmCallException error = ProviderErrors.describe("Gemini API", wrapped);

    assertThat(error.status()).isEqualTo(429);
    assertThat(error.providerCode()).isEqualTo("RESOURCE_EXHAUSTED");
    assertThat(error.retryAfter()).isEqualTo(Duration.ofMillis(41_200));
    assertThat(error.retryable()).isTrue();
    assertThat(error.getMessage())
        .startsWith("Gemini API call failed: HTTP 429 RESOURCE_EXHAUSTED: You exceeded")
        .endsWith("(the provider asks to wait 41s)");
  }

  @Test
  void keepsTheStatusBodyAndRetryAfterOfAnHttpError() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Retry-After", "7");
    HttpClientErrorException http =
        HttpClientErrorException.create(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too Many Requests",
            headers,
            "{\"error\":\"busy\"}".getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8);

    LlmCallException error = ProviderErrors.describe("Ollama", http);

    assertThat(error.getMessage())
        .isEqualTo(
            "Ollama call failed: HTTP 429: {\"error\":\"busy\"} (the provider asks to wait 7s)");
    assertThat(error.retryable()).isTrue();
  }

  @Test
  void treatsClientErrorsAsPermanentAndServerAndNetworkErrorsAsTransient() {
    assertThat(
            ProviderErrors.describe(
                    "Gemini API",
                    new com.google.genai.errors.ApiException(400, "INVALID_ARGUMENT", "Bad"))
                .retryable())
        .isFalse();
    assertThat(
            ProviderErrors.describe(
                    "Gemini API", new com.google.genai.errors.ApiException(500, "INTERNAL", "x"))
                .retryable())
        .isTrue();
    assertThat(
            ProviderErrors.describe(
                    "OpenAI API", new RuntimeException(new IOException("Connection reset")))
                .retryable())
        .isTrue();
  }

  @Test
  void removesTheKeyFromTheMessageAndTheCauses() {
    String key = "sk-synthetic-review-secret-000000";
    RuntimeException wrapped =
        new RuntimeException(
            "Call failed for " + key,
            new com.google.genai.errors.ApiException(
                429, "RESOURCE_EXHAUSTED", "Request for API key " + key + " exceeded quota"));

    LlmCallException error = ProviderErrors.describe("Gemini API", wrapped, List.of(key, ""));

    assertThat(error.getMessage())
        .isEqualTo(
            "Gemini API call failed: HTTP 429 RESOURCE_EXHAUSTED:"
                + " Request for API key [REDACTED] exceeded quota");
    assertThat(error.status()).isEqualTo(429);
    assertThat(error.retryable()).isTrue();
    for (Throwable t = error; t != null; t = t.getCause()) {
      assertThat(t.getMessage()).doesNotContain(key);
      assertThat(t.toString()).doesNotContain(key);
    }
    assertThat(error.getCause())
        .hasMessageContaining("RuntimeException: Call failed for [REDACTED]");
  }

  @Test
  void keepsTheMessageOfAnUnknownError() {
    LlmCallException error =
        ProviderErrors.describe("OpenAI API", new IllegalStateException("Unexpected"));

    assertThat(error.getMessage()).isEqualTo("OpenAI API call failed: Unexpected");
    assertThat(error.retryable()).isFalse();
  }

  @Test
  void readsRetryAfterInSecondsMillisecondsOrAsADate() {
    assertThat(ProviderErrors.retryAfter(List.of("2.5"), List.of()))
        .isEqualTo(Duration.ofMillis(2500));
    assertThat(ProviderErrors.retryAfter(List.of("9"), List.of("1200")))
        .isEqualTo(Duration.ofMillis(1200));
    assertThat(ProviderErrors.retryAfter(List.of("Wed, 21 Oct 2015 07:28:00 GMT"), List.of()))
        .isEqualTo(Duration.ZERO);
    assertThat(ProviderErrors.retryAfter(List.of(), List.of())).isNull();
  }
}
