package br.com.refatorado.kaizent.llm.internal;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmAdapter;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmProfiles;
import br.com.refatorado.kaizent.llm.adapter.Retrying;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LlmProperties.class)
class LlmConfiguration {

  @Bean
  Retrying llmRetrying(LlmProperties properties) {
    LlmProperties.Retry retry = properties.retry();
    return new Retrying(
        retry.maxAttempts(),
        retry.initialBackoff(),
        retry.multiplier(),
        retry.maxWait(),
        duration -> Thread.sleep(duration));
  }

  @Bean
  RoutingLlmGateway llmGateway(LlmProperties properties, List<LlmAdapter> adapters) {
    return new RoutingLlmGateway(properties, adapters);
  }

  @Bean
  LlmProfiles llmProfiles(LlmProperties properties, RoutingLlmGateway gateway) {
    return new LlmProfiles() {
      @Override
      public Map<String, LlmProfile> byStep() {
        return properties.profiles();
      }

      @Override
      public CostType costType(LlmProfile profile) {
        return gateway.costType(profile);
      }
    };
  }
}
