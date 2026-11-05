package br.com.refatorado.kaizent.llm.adapter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.llm.adapter.TestStructuredOutputs;
import br.com.refatorado.kaizent.llm.adapter.cli.CliPreset.Call;
import br.com.refatorado.kaizent.llm.adapter.cli.CliPreset.Output;
import br.com.refatorado.kaizent.llm.adapter.cli.CliPreset.OutputSource;
import br.com.refatorado.kaizent.llm.adapter.cli.CliPreset.PromptMode;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The {@code cli} adapter end to end, with the real {@link DockerCliExecutor} and the fake CLI
 * image (ADR-0020: no real tool or model). Needs Docker, like the Testcontainers tests.
 */
class CliLlmAdapterDockerTest {

  private static final String IMAGE = "kaizent-cli-fake:test";

  record Triage(@NotBlank String category) {}

  @BeforeAll
  static void buildImage() throws Exception {
    Process build =
        new ProcessBuilder(
                "docker",
                "build",
                "--quiet",
                "--tag",
                IMAGE,
                Path.of("src/test/resources/fake-cli").toString())
            .redirectErrorStream(true)
            .start();
    String output = new String(build.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (!build.waitFor(10, TimeUnit.MINUTES) || build.exitValue() != 0) {
      throw new IllegalStateException("docker build failed: " + output);
    }
  }

  @Test
  void answersThroughTheToolsOutputFile() {
    LlmResponse<Triage> response =
        adapter(
                new Call(
                    List.of("fake-cli", "answer", "{outputFile}", "{\"category\":\"bug\"}"),
                    List.of(),
                    PromptMode.STDIN,
                    new Output(OutputSource.FILE, "text")))
            .complete(request(), new LlmProfile("cli", null, "fake"));

    assertThat(response.output()).isEqualTo(new Triage("bug"));
    assertThat(response.usage().costType()).isEqualTo(CostType.SUBSCRIPTION);
    assertThat(response.usage().adapter()).isEqualTo("cli");
  }

  @Test
  void answersThroughTheToolsStandardOutput() {
    LlmResponse<Triage> response =
        adapter(
                new Call(
                    List.of("fake-cli", "answer-stdout", "{\"category\":\"question\"}"),
                    List.of(),
                    PromptMode.STDIN,
                    new Output(OutputSource.STDOUT, "text")))
            .complete(request(), new LlmProfile("cli", null, "fake"));

    assertThat(response.output()).isEqualTo(new Triage("question"));
  }

  private static LlmRequest<Triage> request() {
    return new LlmRequest<>("triage", "system", "a synthetic ticket", Triage.class, Map.of());
  }

  private static CliLlmAdapter adapter(Call llm) {
    CliPreset preset =
        new CliPreset(IMAGE, "test", false, CostType.SUBSCRIPTION, null, List.of(), List.of(), llm);
    DockerCliExecutor executor =
        new DockerCliExecutor(
            new DockerCliExecutor.Settings("docker", 1_000_000, "10001:10001", "1", "256m", 64),
            name -> null);
    return new CliLlmAdapter(
        Map.of("fake", preset),
        List.of(),
        executor,
        TestStructuredOutputs.create(),
        Duration.ofMinutes(1));
  }
}
