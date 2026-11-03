package br.com.refatorado.kaizent.llm.internal;

import br.com.refatorado.kaizent.llm.LlmAdapter;
import br.com.refatorado.kaizent.llm.LlmGateway;
import br.com.refatorado.kaizent.llm.LlmProfiles;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LlmProperties.class)
class LlmConfiguration {

  @Bean
  LlmGateway llmGateway(LlmProperties properties, List<LlmAdapter> adapters) {
    return new RoutingLlmGateway(properties, adapters);
  }

  @Bean
  LlmProfiles llmProfiles(LlmProperties properties) {
    return properties::profiles;
  }
}
