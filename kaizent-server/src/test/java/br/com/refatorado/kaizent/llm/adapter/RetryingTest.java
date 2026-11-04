package br.com.refatorado.kaizent.llm.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import br.com.refatorado.kaizent.llm.LlmCallException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Spec 0004 AC-4: retries with backoff, the provider's suggested wait and the final cause. */
class RetryingTest {

  private final List<Duration> waits = TestRetrying.waits();
  private final Retrying retrying = TestRetrying.recording(waits);

  @Test
  void retriesATransientErrorWithExponentialBackoff() {
    Scripted calls = new Scripted(rateLimited(null), unavailable(), "ok");

    assertThat(retrying.call(calls)).isEqualTo("ok");
    assertThat(waits).containsExactly(Duration.ofSeconds(2), Duration.ofSeconds(4));
  }

  @Test
  void waitsWhatTheProviderAsksWhenItIsLongerThanTheBackoff() {
    Scripted calls = new Scripted(rateLimited(Duration.ofSeconds(41)), "ok");

    assertThat(retrying.call(calls)).isEqualTo("ok");
    assertThat(waits).containsExactly(Duration.ofSeconds(41));
  }

  @Test
  void givesUpAfterTheLastAttemptKeepingTheProviderCause() {
    Scripted calls = new Scripted(rateLimited(null), rateLimited(null), rateLimited(null));

    assertThatExceptionOfType(LlmCallException.class)
        .isThrownBy(() -> retrying.call(calls))
        .withMessage(
            "Gemini API call failed: HTTP 429 RESOURCE_EXHAUSTED: Quota exceeded"
                + " (gave up after 3 attempts)")
        .satisfies(e -> assertThat(e.status()).isEqualTo(429));
    assertThat(waits).hasSize(2);
  }

  @Test
  void doesNotRetryWhenTheProviderAsksForTooLongAWait() {
    Scripted calls = new Scripted(rateLimited(Duration.ofHours(10)), "ok");

    assertThatExceptionOfType(LlmCallException.class)
        .isThrownBy(() -> retrying.call(calls))
        .withMessageEndingWith(
            "(the provider asks to wait 36000s) (not retried: the wait is longer than"
                + " kaizent.llm.retry.max-wait (60s))");
    assertThat(waits).isEmpty();
  }

  @Test
  void doesNotRetryAPermanentError() {
    LlmCallException badKey =
        new LlmCallException("OpenAI API", 401, null, "Invalid API key", null, false, null);
    Scripted calls = new Scripted(badKey, "ok");

    assertThatExceptionOfType(LlmCallException.class)
        .isThrownBy(() -> retrying.call(calls))
        .withMessage("OpenAI API call failed: HTTP 401: Invalid API key");
    assertThat(waits).isEmpty();
  }

  private static LlmCallException rateLimited(Duration retryAfter) {
    return new LlmCallException(
        "Gemini API", 429, "RESOURCE_EXHAUSTED", "Quota exceeded", retryAfter, true, null);
  }

  private static LlmCallException unavailable() {
    return new LlmCallException("Gemini API", 503, "UNAVAILABLE", "Overloaded", null, true, null);
  }

  /** Throws or returns the given outcomes in order. */
  private static final class Scripted implements Supplier<String> {
    private final Deque<Object> outcomes;

    Scripted(Object... outcomes) {
      this.outcomes = new ArrayDeque<>(List.of(outcomes));
    }

    @Override
    public String get() {
      Object next = outcomes.removeFirst();
      if (next instanceof RuntimeException e) {
        throw e;
      }
      return (String) next;
    }
  }
}
