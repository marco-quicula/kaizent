package br.com.refatorado.kaizent.llm.internal;

import br.com.refatorado.kaizent.llm.LlmProfile;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code kaizent.llm.*}: the adapter and model per pipeline step. */
@ConfigurationProperties("kaizent.llm")
record LlmProperties(Map<String, LlmProfile> profiles) {

  LlmProperties {
    profiles = profiles == null ? Map.of() : Map.copyOf(profiles);
  }
}
