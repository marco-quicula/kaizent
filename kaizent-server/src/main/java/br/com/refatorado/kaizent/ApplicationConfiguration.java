package br.com.refatorado.kaizent;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Application-wide infrastructure shared by the modules. */
@Configuration(proxyBeanMethods = false)
class ApplicationConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  /**
   * Enables the intake poller and the run worker. Tests turn it off ({@code
   * kaizent.scheduling.enabled=false}) and drive both directly.
   */
  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  @ConditionalOnProperty(
      name = "kaizent.scheduling.enabled",
      havingValue = "true",
      matchIfMissing = true)
  static class SchedulingConfiguration {}
}
