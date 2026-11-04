package br.com.refatorado.kaizent.llm.internal;

import br.com.refatorado.kaizent.llm.LlmProfile;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code kaizent.llm.*}.
 *
 * @param profiles the adapter and model per pipeline step
 * @param pricing the price per model id, in US dollars per million tokens. Model ids with dots need
 *     brackets in YAML keys, for example {@code "[gemini-3.6-flash]"}
 */
@ConfigurationProperties("kaizent.llm")
record LlmProperties(Map<String, LlmProfile> profiles, Map<String, Price> pricing, Retry retry) {

  LlmProperties {
    profiles = profiles == null ? Map.of() : Map.copyOf(profiles);
    pricing = pricing == null ? Map.of() : Map.copyOf(pricing);
    retry =
        retry == null ? new Retry(3, Duration.ofSeconds(2), 2.0, Duration.ofSeconds(60)) : retry;
  }

  /**
   * {@code kaizent.llm.retry.*}: transient errors (HTTP 429, 5xx, network) are retried with
   * exponential backoff (spec 0004 AC-4).
   *
   * @param maxAttempts attempts per call, including the first
   * @param initialBackoff the wait before the second attempt
   * @param multiplier how much the wait grows after each attempt
   * @param maxWait a call whose provider asks for a longer wait is not retried
   */
  record Retry(
      @DefaultValue("3") int maxAttempts,
      @DefaultValue("2s") Duration initialBackoff,
      @DefaultValue("2.0") double multiplier,
      @DefaultValue("60s") Duration maxWait) {}

  /** A model's price in US dollars per million input and output tokens. */
  record Price(BigDecimal inputPerMillion, BigDecimal outputPerMillion) {}
}
