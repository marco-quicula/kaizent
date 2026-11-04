package br.com.refatorado.kaizent.llm.adapter;

import br.com.refatorado.kaizent.llm.LlmCallException;
import br.com.refatorado.kaizent.llm.LlmException;
import java.time.Duration;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retries transient model call errors (HTTP 429, 5xx, network) with exponential backoff, up to
 * {@code kaizent.llm.retry.max-attempts} (spec 0004 AC-4). A wait suggested by the provider is
 * respected when it is longer than the backoff; when it is longer than {@code max-wait}, the call
 * is not retried, because the limit (for example, a daily quota) will not reset soon. The final
 * error keeps the provider's cause.
 */
public final class Retrying {

  private static final Logger log = LoggerFactory.getLogger(Retrying.class);

  /** Waits; tests replace it. */
  @FunctionalInterface
  public interface Sleeper {
    void sleep(Duration duration) throws InterruptedException;
  }

  private final int maxAttempts;
  private final Duration initialBackoff;
  private final double multiplier;
  private final Duration maxWait;
  private final Sleeper sleeper;

  public Retrying(
      int maxAttempts,
      Duration initialBackoff,
      double multiplier,
      Duration maxWait,
      Sleeper sleeper) {
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("kaizent.llm.retry.max-attempts must be at least 1");
    }
    this.maxAttempts = maxAttempts;
    this.initialBackoff = initialBackoff;
    this.multiplier = multiplier;
    this.maxWait = maxWait;
    this.sleeper = sleeper;
  }

  /** Runs the call, retrying transient {@link LlmCallException}s. */
  public <T> T call(Supplier<T> call) {
    Duration backoff = initialBackoff;
    for (int attempt = 1; ; attempt++) {
      try {
        return call.get();
      } catch (LlmCallException e) {
        if (!e.retryable()) {
          throw e;
        }
        if (attempt >= maxAttempts) {
          throw maxAttempts == 1 ? e : e.withNote("gave up after " + maxAttempts + " attempts");
        }
        Duration wait =
            e.retryAfter() != null && e.retryAfter().compareTo(backoff) > 0
                ? e.retryAfter()
                : backoff;
        if (wait.compareTo(maxWait) > 0) {
          throw e.withNote(
              "not retried: the wait is longer than kaizent.llm.retry.max-wait ("
                  + maxWait.toSeconds()
                  + "s)");
        }
        log.info(
            "Model call failed ({}); attempt {} of {} in {} ms",
            e.getMessage(),
            attempt + 1,
            maxAttempts,
            wait.toMillis());
        try {
          sleeper.sleep(wait);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw new LlmException("Interrupted while waiting to retry a model call", e);
        }
        backoff = Duration.ofMillis(Math.round(backoff.toMillis() * multiplier));
      }
    }
  }
}
