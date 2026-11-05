package br.com.refatorado.kaizent.llm.internal;

import br.com.refatorado.kaizent.llm.CliExecutor;
import br.com.refatorado.kaizent.llm.LlmAdapter;
import br.com.refatorado.kaizent.llm.adapter.StructuredOutputs;
import br.com.refatorado.kaizent.llm.adapter.cli.CliEvents;
import br.com.refatorado.kaizent.llm.adapter.cli.CliLlmAdapter;
import br.com.refatorado.kaizent.llm.adapter.cli.DockerCliExecutor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** The {@link CliExecutor}; Docker is needed only when a CLI adapter runs a call (ADR-0030). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CliProperties.class)
class CliConfiguration {

  @Bean
  CliExecutor cliExecutor(CliProperties properties, Environment environment) {
    return new DockerCliExecutor(
        new DockerCliExecutor.Settings(
            properties.docker(),
            properties.maxOutput().toBytes(),
            properties.user(),
            properties.limits().cpus(),
            properties.limits().memory(),
            properties.limits().pids()),
        // Values come from .env or the environment; never from the whole host environment.
        environment::getProperty);
  }

  @Bean
  LlmAdapter cliLlmAdapter(
      CliProperties properties,
      ObjectProvider<CliEvents> events,
      CliExecutor executor,
      StructuredOutputs structuredOutputs) {
    return new CliLlmAdapter(
        properties.presets(),
        events.orderedStream().toList(),
        executor,
        structuredOutputs,
        properties.timeout());
  }
}
