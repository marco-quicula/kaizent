package br.com.refatorado.kaizent.workspace.internal;

import br.com.refatorado.kaizent.workspace.Workspace;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WorkspaceProperties.class)
class WorkspaceConfiguration {

  private static final Logger log = LoggerFactory.getLogger(WorkspaceConfiguration.class);

  @Bean
  Workspace workspace(WorkspaceProperties properties) {
    Workspace workspace = Workspace.resolve(properties.path(), Path.of(""));
    log.info("Workspace: {}", workspace.root());
    return workspace;
  }
}
