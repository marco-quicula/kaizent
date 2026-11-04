package br.com.refatorado.kaizent.llm.adapter.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import br.com.refatorado.kaizent.llm.CliCommand;
import br.com.refatorado.kaizent.llm.CliException;
import br.com.refatorado.kaizent.llm.CliException.Kind;
import br.com.refatorado.kaizent.llm.CliResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The container contract of ADR-0030 (spec 0004 AC-6), against a fake CLI image built from {@code
 * src/test/resources/fake-cli}. Like the Testcontainers tests, it needs Docker, and it behaves the
 * same on every operating system.
 */
class DockerCliExecutorTest {

  private static final String IMAGE = "kaizent-cli-fake:test";
  private static final Duration TIMEOUT = Duration.ofSeconds(60);
  private static final String VOLUME = "kaizent-cli-test-" + UUID.randomUUID();

  private final Map<String, String> configuration =
      Map.of("ALLOWED_KEY", "allowed-value", "OTHER_KEY", "other-secret-value");
  private DockerCliExecutor executor;

  @TempDir Path workspace;

  @BeforeAll
  static void buildImage() throws Exception {
    docker("build", "--quiet", "--tag", IMAGE, Path.of("src/test/resources/fake-cli").toString());
  }

  @AfterAll
  static void removeVolume() throws Exception {
    docker("volume", "rm", "--force", VOLUME);
  }

  @BeforeEach
  void setUp() throws IOException {
    executor = executor(2 * 1024 * 1024);
    write("knowledge/product/owners.md", "Owners are searched by last name.");
    write("knowledge/internal/secret.md", "not granted");
    write("evals/cases/PC-001.yaml", "expected answer");
    write("inbox/PC-001.md", "a ticket");
  }

  @Test
  void passesTheStdinAndReturnsTheOutput() {
    CliResult result =
        executor.execute(
            CliCommand.builder(IMAGE, TIMEOUT)
                .args("fake-cli", "echo-stdin")
                .stdin("the prompt")
                .build());

    assertThat(result.stdout()).isEqualTo("the prompt");
    assertThat(result.truncated()).isFalse();
  }

  @Test
  void runsAsTheNonRootUser() {
    assertThat(run("fake-cli", "id").stdout().strip()).isEqualTo("10001");
  }

  @Test
  void mountsOnlyTheGrantedPathsReadOnly() {
    CliCommand.Builder granted =
        CliCommand.builder(IMAGE, TIMEOUT)
            .workspace(workspace)
            .grant(Path.of("knowledge", "product"));

    CliResult listing = executor.execute(granted.args("fake-cli", "ls", "/workspace").build());
    assertThat(listing.stdout().lines())
        .containsExactly(
            "/workspace",
            "/workspace/knowledge",
            "/workspace/knowledge/product",
            "/workspace/knowledge/product/owners.md");

    CliResult read =
        executor.execute(
            CliCommand.builder(IMAGE, TIMEOUT)
                .workspace(workspace)
                .grant(Path.of("knowledge", "product"))
                .args("fake-cli", "cat", "/workspace/knowledge/product/owners.md")
                .build());
    assertThat(read.stdout()).isEqualTo("Owners are searched by last name.");

    assertThatExceptionOfType(CliException.class)
        .isThrownBy(() ->
            executor.execute(
                CliCommand.builder(IMAGE, TIMEOUT)
                    .workspace(workspace)
                    .grant(Path.of("knowledge", "product"))
                    .args("fake-cli", "write", "/workspace/knowledge/product/new.md", "x")
                    .build()))
        .satisfies(e -> assertThat(e.kind()).isEqualTo(Kind.NON_ZERO_EXIT))
        .withMessageContaining("Read-only file system");
    assertThat(workspace.resolve("knowledge/product/new.md")).doesNotExist();
  }

  @Test
  void neverShowsTheEvaluationCasesOrOtherWorkspaceFiles() {
    for (String path :
        List.of(
            "/workspace/evals/cases/PC-001.yaml",
            "/workspace/knowledge/internal/secret.md",
            "/workspace/inbox/PC-001.md")) {
      assertThatExceptionOfType(CliException.class)
          .isThrownBy(() ->
              executor.execute(
                  CliCommand.builder(IMAGE, TIMEOUT)
                      .workspace(workspace)
                      .grant(Path.of("knowledge", "product"))
                      .args("fake-cli", "cat", path)
                      .build()))
          .withMessageContaining("No such file or directory");
    }
  }

  @Test
  void rejectsAGrantedPathOutsideTheWorkspace() throws IOException {
    Path outside = Files.createTempDirectory("kaizent-outside-");
    try {
      Files.createSymbolicLink(workspace.resolve("link"), outside);
    } catch (UnsupportedOperationException | IOException e) {
      // No symbolic links here (Windows without the privilege): the other cases still run.
    }
    for (Path path : List.of(Path.of("..", "x"), outside, Path.of("knowledge", "..", "evals"))) {
      assertThatIllegalArgumentException()
          .isThrownBy(() ->
              executor.execute(
                  CliCommand.builder(IMAGE, TIMEOUT)
                      .workspace(workspace)
                      .grant(path)
                      .args("fake-cli", "id")
                      .build()));
    }
    if (Files.isSymbolicLink(workspace.resolve("link"))) {
      assertThatIllegalArgumentException()
          .isThrownBy(() ->
              executor.execute(
                  CliCommand.builder(IMAGE, TIMEOUT)
                      .workspace(workspace)
                      .grant(Path.of("link"))
                      .args("fake-cli", "id")
                      .build()))
          .withMessageContaining("leaves the workspace");
    }
  }

  @Test
  void hasAReadOnlyRootAndAWritableScratchThatIsDeletedAfterwards() {
    assertThatExceptionOfType(CliException.class)
        .isThrownBy(() -> run("fake-cli", "write", "/etc/kaizent", "x"))
        .withMessageContaining("Read-only file system");

    CliResult result =
        executor.execute(
            CliCommand.builder(IMAGE, TIMEOUT)
                .scratchFile("prompt.md", "from Kaizent")
                .args("fake-cli", "write", "/scratch/answer.json", "{\"ok\":true}")
                .outputFile("answer.json")
                .outputFile("missing.json")
                .build());
    assertThat(result.files()).containsExactly(Map.entry("answer.json", "{\"ok\":true}"));

    CliResult input =
        executor.execute(
            CliCommand.builder(IMAGE, TIMEOUT)
                .scratchFile("prompt.md", "from Kaizent")
                .args("fake-cli", "cat", "/scratch/prompt.md")
                .build());
    assertThat(input.stdout()).isEqualTo("from Kaizent");
    // Each call has its own scratch folder.
    assertThatExceptionOfType(CliException.class)
        .isThrownBy(() -> run("fake-cli", "cat", "/scratch/answer.json"));
  }

  @Test
  void passesOnlyTheDeclaredEnvironmentVariables() {
    CliResult result =
        executor.execute(
            CliCommand.builder(IMAGE, TIMEOUT).env("ALLOWED_KEY").args("fake-cli", "env").build());

    assertThat(result.stdout()).contains("ALLOWED_KEY=allowed-value");
    // Only the declared variable, plus what the image and the shell set themselves.
    assertThat(result.stdout().lines().map(line -> line.substring(0, line.indexOf('='))))
        .containsExactlyInAnyOrder("ALLOWED_KEY", "HOME", "HOSTNAME", "PATH", "PWD", "SHLVL");
  }

  @Test
  void keepsTheLoginInTheCredentialVolume() {
    executor.execute(
        CliCommand.builder(IMAGE, TIMEOUT)
            .credentials(VOLUME, "/home/kaizent/.tool")
            .args("fake-cli", "write", "/home/kaizent/.tool/login", "token")
            .build());

    CliResult next =
        executor.execute(
            CliCommand.builder(IMAGE, TIMEOUT)
                .credentials(VOLUME, "/home/kaizent/.tool")
                .args("fake-cli", "cat", "/home/kaizent/.tool/login")
                .build());
    assertThat(next.stdout()).isEqualTo("token");
  }

  @Test
  void stopsTheContainerWhenTheTimeoutIsReached() throws Exception {
    long start = System.nanoTime();

    assertThatExceptionOfType(CliException.class)
        .isThrownBy(() ->
            executor.execute(
                CliCommand.builder(IMAGE, Duration.ofSeconds(3))
                    .args("fake-cli", "sleep", "120")
                    .build()))
        .satisfies(e -> assertThat(e.kind()).isEqualTo(Kind.TIMEOUT))
        .withMessageContaining("timed out after 3s; the container was stopped");

    assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(60));
    String running = docker("ps", "--quiet", "--filter", "ancestor=" + IMAGE);
    assertThat(running).isBlank();
  }

  @Test
  void raisesANonZeroExitWithAnExcerptOfStderrAndNoSecrets() {
    assertThatExceptionOfType(CliException.class)
        .isThrownBy(() ->
            executor.execute(
                CliCommand.builder(IMAGE, TIMEOUT)
                    .env("ALLOWED_KEY")
                    .args("fake-cli", "fail", "3", "bad key allowed-value rejected")
                    .build()))
        .satisfies(e -> {
          assertThat(e.kind()).isEqualTo(Kind.NON_ZERO_EXIT);
          assertThat(e.exitCode()).isEqualTo(3);
        })
        .withMessage("CLI call in " + IMAGE + " exited with code 3: bad key [REDACTED] rejected");
  }

  @Test
  void capsTheOutput() {
    CliResult result =
        executor(1000)
            .execute(CliCommand.builder(IMAGE, TIMEOUT).args("fake-cli", "big", "3000000").build());

    assertThat(result.stdout()).hasSize(1000);
    assertThat(result.truncated()).isTrue();
  }

  @Test
  void failsClearlyWhenDockerIsMissing() {
    DockerCliExecutor noDocker =
        new DockerCliExecutor(
            new DockerCliExecutor.Settings(
                "kaizent-no-such-docker", 1000, "10001:10001", "1", "256m", 64),
            name -> null);

    assertThatExceptionOfType(CliException.class)
        .isThrownBy(() -> noDocker.execute(CliCommand.builder(IMAGE, TIMEOUT).args("x").build()))
        .satisfies(e -> assertThat(e.kind()).isEqualTo(Kind.DOCKER_UNAVAILABLE))
        .withMessageContaining("Docker is not available");
  }

  @Test
  void failsClearlyWhenDockerCannotRunTheImage() {
    assertThatExceptionOfType(CliException.class)
        .isThrownBy(() ->
            executor.execute(
                CliCommand.builder("kaizent-cli-missing:none", TIMEOUT).args("x").build()))
        .satisfies(e -> assertThat(e.kind()).isEqualTo(Kind.DOCKER_UNAVAILABLE))
        .withMessageContaining("Docker could not run the container");
  }

  @Test
  void refusesARootUser() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new DockerCliExecutor.Settings("docker", 1, "0:0", "1", "1g", 1));
  }

  private CliResult run(String... args) {
    return executor.execute(CliCommand.builder(IMAGE, TIMEOUT).args(args).build());
  }

  private DockerCliExecutor executor(long maxOutput) {
    return new DockerCliExecutor(
        new DockerCliExecutor.Settings("docker", maxOutput, "10001:10001", "1", "256m", 64),
        configuration::get);
  }

  private void write(String relative, String content) throws IOException {
    Path file = workspace.resolve(relative);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content);
  }

  private static String docker(String... args) throws Exception {
    List<String> command = new java.util.ArrayList<>(List.of("docker"));
    command.addAll(List.of(args));
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (!process.waitFor(10, TimeUnit.MINUTES) || process.exitValue() != 0) {
      throw new IllegalStateException("docker " + String.join(" ", args) + " failed: " + output);
    }
    return output;
  }
}
