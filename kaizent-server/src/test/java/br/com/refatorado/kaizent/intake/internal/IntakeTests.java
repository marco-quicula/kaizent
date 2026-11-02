package br.com.refatorado.kaizent.intake.internal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.Database;
import br.com.refatorado.kaizent.KaizentApplication;
import br.com.refatorado.kaizent.KaizentIntegrationTest;
import br.com.refatorado.kaizent.TestcontainersConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.FileSystemUtils;

/** Spec 0002 AC-1 and AC-2: tickets in the inbox become problems and runs, exactly once. */
@KaizentIntegrationTest
@TestPropertySource(properties = "kaizent.workspace.path=" + IntakeTests.WORKSPACE)
class IntakeTests {

  static final String WORKSPACE = "target/intake-tests-workspace";

  @Autowired private IntakePoller poller;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() throws IOException {
    Database.clear(jdbc);
    FileSystemUtils.deleteRecursively(Path.of(WORKSPACE));
    Files.createDirectories(Path.of(WORKSPACE, "inbox"));
    Files.writeString(
        Path.of(WORKSPACE, "inbox", "PC-001.md"),
        "---\nid: PC-001\n---\nThe telephone field rejects my number.\n");
  }

  @Test
  void createsAProblemAndAReceivedRunForANewTicket() {
    assertThat(poller.poll()).isEqualTo(1);

    Map<String, Object> row =
        jdbc.queryForMap(
            """
            SELECT p.source, p.external_key, p.title, r.status
            FROM problem p JOIN run r ON r.problem_id = p.id
            """);
    assertThat(row)
        .containsEntry("source", "file-inbox")
        .containsEntry("external_key", "PC-001")
        .containsEntry("title", "The telephone field rejects my number.")
        .containsEntry("status", "RECEIVED");
  }

  @Test
  void neverCreatesASecondRunForTheSameTicket() {
    assertThat(poller.poll()).isEqualTo(1);
    assertThat(poller.poll()).isZero();

    assertThat(runCount()).isEqualTo(1);
  }

  @Test
  void neverCreatesASecondRunAfterARestart() {
    assertThat(poller.poll()).isEqualTo(1);

    try (ConfigurableApplicationContext restarted = startAnotherInstance()) {
      assertThat(restarted.getBean(IntakePoller.class).poll()).isZero();
    }

    assertThat(runCount()).isEqualTo(1);
  }

  private int runCount() {
    return jdbc.queryForObject("SELECT count(*) FROM run", Integer.class);
  }

  /** A fresh application context on the same database, as after a restart. */
  private static ConfigurableApplicationContext startAnotherInstance() {
    var postgres = TestcontainersConfiguration.POSTGRES;
    return new SpringApplicationBuilder(KaizentApplication.class)
        .profiles("test")
        .web(WebApplicationType.NONE)
        // Command-line arguments, so that they take precedence over application.yaml.
        .run(
            "--kaizent.workspace.path=" + WORKSPACE,
            "--spring.datasource.url=" + postgres.getJdbcUrl(),
            "--spring.datasource.username=" + postgres.getUsername(),
            "--spring.datasource.password=" + postgres.getPassword());
  }
}
