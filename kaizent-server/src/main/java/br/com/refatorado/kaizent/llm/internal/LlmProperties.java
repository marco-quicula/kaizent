package br.com.refatorado.kaizent.llm.internal;

import br.com.refatorado.kaizent.llm.LlmProfile;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code kaizent.llm.*}.
 *
 * @param profiles the adapter and model per pipeline step
 * @param pricing the price per model id, in US dollars per million tokens. Model ids with dots need
 *     brackets in YAML keys, for example {@code "[gemini-3.6-flash]"}
 */
@ConfigurationProperties("kaizent.llm")
record LlmProperties(Map<String, LlmProfile> profiles, Map<String, Price> pricing) {

  LlmProperties {
    profiles = profiles == null ? Map.of() : Map.copyOf(profiles);
    pricing = pricing == null ? Map.of() : Map.copyOf(pricing);
  }

  /** A model's price in US dollars per million input and output tokens. */
  record Price(BigDecimal inputPerMillion, BigDecimal outputPerMillion) {}
}
