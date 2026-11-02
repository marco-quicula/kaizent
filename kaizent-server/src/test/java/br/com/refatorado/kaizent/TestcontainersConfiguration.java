package br.com.refatorado.kaizent;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL 18 for integration tests (ADR-0009, ADR-0018). One container is started per JVM and
 * shared by every test context, so tests must not assume an empty database between classes. It is
 * not a bean on purpose: closing a context (for example with {@code @DirtiesContext}) must not stop
 * it. Testcontainers removes it when the JVM exits.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  public static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:18"));

  static {
    POSTGRES.start();
  }

  @Bean
  DynamicPropertyRegistrar postgresProperties() {
    return registry -> {
      registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
      registry.add("spring.datasource.username", POSTGRES::getUsername);
      registry.add("spring.datasource.password", POSTGRES::getPassword);
    };
  }
}
