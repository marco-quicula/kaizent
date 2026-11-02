package br.com.refatorado.kaizent;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL 18 for integration tests (ADR-0009, ADR-0018). A single container is shared by all
 * test contexts of the JVM, so tests must not assume an empty database between classes.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  public static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:18"));

  @Bean(destroyMethod = "")
  @ServiceConnection
  PostgreSQLContainer postgres() {
    return POSTGRES;
  }
}
