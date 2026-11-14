package br.com.refatorado.kaizent.llm.internal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.llm.CliCommand;
import br.com.refatorado.kaizent.llm.CliResult;
import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.llm.adapter.TestStructuredOutputs;
import br.com.refatorado.kaizent.llm.adapter.cli.CliLlmAdapter;
import br.com.refatorado.kaizent.llm.adapter.cli.CodexEvents;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/** The bundled presets of cli-presets.yaml bind and make the expected commands (spec 0004 AC-9). */
class BundledCliPresetsTest {

  record Answer(String ok) {}

  /** The command that the adapter makes for the preset, with the executor faked. */
  private static Run runPreset(
      String preset, String model, String stdout, Map<String, String> files, Answer expected)
      throws Exception {
    StandardEnvironment environment = new StandardEnvironment();
    new YamlPropertySourceLoader()
        .load("cli-presets", new ClassPathResource("cli-presets.yaml"))
        .forEach(environment.getPropertySources()::addLast);
    CliProperties properties =
        Binder.get(environment).bindOrCreate("kaizent.cli", CliProperties.class);
    List<CliCommand> commands = new ArrayList<>();
    CliLlmAdapter adapter =
        new CliLlmAdapter(
            properties.presets(),
            List.of(new CodexEvents()),
            command -> {
              commands.add(command);
              return new CliResult(stdout, "", false, files, Duration.ofSeconds(1));
            },
            TestStructuredOutputs.create(),
            properties.timeout());
    LlmProfile profile = new LlmProfile("cli", model, preset);

    assertThat(adapter.invalidProfile(profile)).isEmpty();
    var response =
        adapter.complete(
            new LlmRequest<>("triage", "system", "user", Answer.class, Map.of()), profile);

    assertThat(response.output()).isEqualTo(expected);
    assertThat(ConfigurationPropertySources.get(environment)).isNotNull();
    return new Run(commands.getFirst(), response.usage());
  }

  record Run(CliCommand command, Usage usage) {}

  @Test
  void theCodexPresetRunsKaizentCodexWithTheValidatedFlags() throws Exception {
    Run run =
        runPreset(
            "codex",
            null,
            "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":10,\"output_tokens\":2}}",
            Map.of("answer.txt", "{\"ok\":\"yes\"}"),
            new Answer("yes"));
    CliCommand command = run.command();

    assertThat(run.usage().inputTokens()).isEqualTo(10);
    assertThat(run.usage().costType()).isEqualTo(CostType.SUBSCRIPTION);
    assertThat(command.image()).isEqualTo("kaizent-cli-codex");
    assertThat(command.args())
        .containsExactly(
            "kaizent-codex",
            "exec",
            "--json",
            "--ephemeral",
            "--sandbox",
            "read-only",
            "--skip-git-repo-check",
            "--disable",
            "shell_tool",
            "-c",
            "web_search=\"disabled\"",
            "--output-schema",
            "/scratch/schema.json",
            "--output-last-message",
            "/scratch/answer.txt",
            "-");
    assertThat(command.credentials())
        .isEqualTo(new CliCommand.Credentials("kaizent-cli-codex", "/home/kaizent/.codex-login"));
    assertThat(command.env()).isEmpty();
    assertThat(command.grantedPaths()).isEmpty();
  }

  @Test
  void theClaudeCodePresetPassesTheTokenByNameAndTurnsTheToolsOff() throws Exception {
    Run run =
        runPreset("claude-code", "sonnet", "{\"ok\":\"yes\"}", Map.of(), new Answer("yes"));
    CliCommand command = run.command();

    assertThat(command.image()).isEqualTo("kaizent-cli-claude-code");
    assertThat(command.args())
        .containsExactly(
            "kaizent-claude",
            "-p",
            "--output-format",
            "text",
            "--tools",
            "",
            "--no-session-persistence",
            "--json-schema-file",
            "/scratch/schema.json",
            "--model",
            "sonnet");
    assertThat(command.stdin()).startsWith("system\n\nuser");
    // The token reaches the container by name only: no login volume, and never in the arguments.
    assertThat(command.env()).containsExactly("CLAUDE_CODE_OAUTH_TOKEN");
    assertThat(command.credentials()).isNull();
    assertThat(String.join(" ", command.args())).doesNotContain("TOKEN");
    assertThat(run.usage().costType()).isEqualTo(CostType.SUBSCRIPTION);
    assertThat(run.usage().inputTokens()).isNull();
  }

  @Test
  void theAntigravityPresetPassesTheKeyByNameAndNeverSkipsPermissions() throws Exception {
    Run run =
        runPreset("antigravity", null, "{\"ok\":\"yes\"}", Map.of(), new Answer("yes"));
    CliCommand command = run.command();

    assertThat(command.image()).isEqualTo("kaizent-cli-antigravity");
    assertThat(command.args())
        .containsExactly(
            "kaizent-agy",
            "-p",
            "--output-format",
            "text",
            "--json-schema",
            "/scratch/schema.json",
            "--print-timeout",
            "290s");
    assertThat(command.args()).doesNotContain("--dangerously-skip-permissions");
    assertThat(command.stdin()).startsWith("system\n\nuser");
    assertThat(command.env()).containsExactly("GEMINI_API_KEY");
    assertThat(command.credentials()).isNull();
    assertThat(run.usage().costType()).isEqualTo(CostType.API);
  }

  @Test
  void thePresetsThatAreNotValidatedSayso() throws Exception {
    StandardEnvironment environment = new StandardEnvironment();
    new YamlPropertySourceLoader()
        .load("cli-presets", new ClassPathResource("cli-presets.yaml"))
        .forEach(environment.getPropertySources()::addLast);
    CliProperties properties =
        Binder.get(environment).bindOrCreate("kaizent.cli", CliProperties.class);

    assertThat(properties.presets().get("codex").validated()).isTrue();
    assertThat(properties.presets().get("claude-code").validated()).isFalse();
    assertThat(properties.presets().get("claude-code").toolVersion()).isEqualTo("2.1.319");
    assertThat(properties.presets().get("antigravity").validated()).isFalse();
    assertThat(properties.presets().get("antigravity").toolVersion()).isEqualTo("1.5.9");
  }
}
