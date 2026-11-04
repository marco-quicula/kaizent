package br.com.refatorado.kaizent.llm.adapter.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import br.com.refatorado.kaizent.llm.CliCommand;
import br.com.refatorado.kaizent.llm.CliExecutor;
import br.com.refatorado.kaizent.llm.CliResult;
import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmException;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.llm.adapter.TestStructuredOutputs;
import br.com.refatorado.kaizent.llm.adapter.cli.CliPreset.Call;
import br.com.refatorado.kaizent.llm.adapter.cli.CliPreset.Output;
import br.com.refatorado.kaizent.llm.adapter.cli.CliPreset.OutputSource;
import br.com.refatorado.kaizent.llm.adapter.cli.CliPreset.PromptMode;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The {@code cli} adapter with a recording executor: no Docker (spec 0004 AC-7). */
class CliLlmAdapterTest {

  record Triage(@NotBlank String category) {}

  private final LlmRequest<Triage> request =
      new LlmRequest<>("triage", "the system prompt", "the user prompt", Triage.class, Map.of());
  private final List<CliCommand> commands = new ArrayList<>();

  @Test
  void runsThePresetWithTheSchemaAndReadsTheAnswerFile() {
    CliLlmAdapter adapter = adapter(preset(OutputSource.FILE, "text"), "{\"category\":\"bug\"}");

    LlmResponse<Triage> response =
        adapter.complete(request, new LlmProfile("cli", "a-model", "tool"));

    assertThat(response.output()).isEqualTo(new Triage("bug"));
    assertThat(response.usage())
        .isEqualTo(new Usage(null, null, null, CostType.SUBSCRIPTION, "cli", "a-model"));
    CliCommand command = commands.getFirst();
    assertThat(command.image()).isEqualTo("kaizent-cli-tool");
    assertThat(command.args())
        .containsExactly(
            "tool",
            "run",
            "--schema",
            "/scratch/schema.json",
            "--out",
            "/scratch/answer.txt",
            "--model",
            "a-model");
    assertThat(command.stdin()).startsWith("the system prompt\n\nthe user prompt");
    assertThat(command.scratchFiles().get("schema.json")).contains("\"category\"");
    assertThat(command.outputFiles()).containsExactly("answer.txt");
    // A single structured call grants no workspace path (spec 0004 AC-7).
    assertThat(command.grantedPaths()).isEmpty();
    assertThat(command.workspace()).isNull();
    assertThat(command.credentials()).isEqualTo(new CliCommand.Credentials("vol", "/home/x/.tool"));
    assertThat(command.env()).containsExactly("TOOL_KEY");
    assertThat(command.tmpfs()).containsExactly("/home/x/.cache");
  }

  @Test
  void leavesTheModelOutWhenTheProfileHasNone() {
    adapter(preset(OutputSource.FILE, "text"), "{\"category\":\"bug\"}")
        .complete(request, new LlmProfile("cli", null, "tool"));

    assertThat(commands.getFirst().args()).doesNotContain("--model");
  }

  @Test
  void readsTheAnswerAndTheTokensFromTheStandardOutput() {
    CliEvents counting =
        new CliEvents() {
          @Override
          public String name() {
            return "counting";
          }

          @Override
          public Optional<String> answer(String stdout) {
            return Optional.of(stdout.lines().toList().getLast());
          }

          @Override
          public Tokens tokens(String stdout) {
            return new Tokens(120, 30);
          }
        };
    CliLlmAdapter adapter =
        new CliLlmAdapter(
            Map.of("tool", preset(OutputSource.STDOUT, "counting")),
            List.of(counting),
            recording("{\"progress\":1}\n{\"category\":\"bug\"}", Map.of()),
            TestStructuredOutputs.create(),
            Duration.ofMinutes(1));

    LlmResponse<Triage> response = adapter.complete(request, new LlmProfile("cli", null, "tool"));

    assertThat(response.output()).isEqualTo(new Triage("bug"));
    assertThat(response.usage().inputTokens()).isEqualTo(120);
    assertThat(response.usage().outputTokens()).isEqualTo(30);
    assertThat(commands.getFirst().outputFiles()).isEmpty();
  }

  @Test
  void writesThePromptToAFileWhenThePresetSaysSo() {
    CliPreset preset =
        new CliPreset(
            "kaizent-cli-tool",
            "1",
            false,
            CostType.SUBSCRIPTION,
            null,
            List.of(),
            List.of(),
            new Call(
                List.of("tool", "{promptFile}"),
                List.of(),
                PromptMode.FILE,
                new Output(OutputSource.STDOUT, "text")));
    CliLlmAdapter adapter =
        new CliLlmAdapter(
            Map.of("tool", preset),
            List.of(),
            recording("{\"category\":\"bug\"}", Map.of()),
            TestStructuredOutputs.create(),
            Duration.ofMinutes(1));

    adapter.complete(request, new LlmProfile("cli", null, "tool"));

    CliCommand command = commands.getFirst();
    assertThat(command.args()).containsExactly("tool", "/scratch/prompt.md");
    assertThat(command.stdin()).isNull();
    assertThat(command.scratchFiles().get("prompt.md")).startsWith("the system prompt");
  }

  @Test
  void rejectsAProfileWithoutAKnownPreset() {
    CliLlmAdapter adapter = adapter(preset(OutputSource.FILE, "text"), "{}");

    assertThat(adapter.invalidProfile(new LlmProfile("cli", null, null)))
        .hasValueSatisfying(reason -> assertThat(reason).contains("needs a preset"));
    assertThat(adapter.invalidProfile(new LlmProfile("cli", null, "other")))
        .hasValueSatisfying(reason -> assertThat(reason).contains("unknown CLI preset other"));
    assertThat(adapter.invalidProfile(new LlmProfile("cli", null, "tool"))).isEmpty();
    assertThatExceptionOfType(LlmException.class)
        .isThrownBy(() -> adapter.complete(request, new LlmProfile("cli", null, "other")));
  }

  @Test
  void rejectsAPresetWithAnUnknownOutputReader() {
    CliLlmAdapter adapter = adapter(preset(OutputSource.STDOUT, "missing"), "{}");

    assertThat(adapter.invalidProfile(new LlmProfile("cli", null, "tool")))
        .hasValueSatisfying(reason -> assertThat(reason).contains("missing"));
  }

  @Test
  void takesTheCostTypeFromThePreset() {
    CliLlmAdapter adapter = adapter(preset(OutputSource.FILE, "text"), "{}");

    assertThat(adapter.costType(new LlmProfile("cli", null, "tool")))
        .isEqualTo(CostType.SUBSCRIPTION);
  }

  private CliLlmAdapter adapter(CliPreset preset, String answer) {
    return new CliLlmAdapter(
        Map.of("tool", preset),
        List.of(),
        recording("", Map.of(CliLlmAdapter.OUTPUT_FILE, answer)),
        TestStructuredOutputs.create(),
        Duration.ofMinutes(1));
  }

  private CliExecutor recording(String stdout, Map<String, String> files) {
    return command -> {
      commands.add(command);
      return new CliResult(stdout, "", false, files, Duration.ofSeconds(1));
    };
  }

  private static CliPreset preset(OutputSource source, String events) {
    return new CliPreset(
        "kaizent-cli-tool",
        "1.0",
        false,
        CostType.SUBSCRIPTION,
        new CliPreset.Credentials("vol", "/home/x/.tool"),
        List.of("TOOL_KEY"),
        List.of("/home/x/.cache"),
        new Call(
            List.of("tool", "run", "--schema", "{schemaFile}", "--out", "{outputFile}"),
            List.of("--model", "{model}"),
            PromptMode.STDIN,
            new Output(source, events)));
  }
}
