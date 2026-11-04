package br.com.refatorado.kaizent.llm.internal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.llm.CliCommand;
import br.com.refatorado.kaizent.llm.CliResult;
import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
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

  @Test
  void theCodexPresetRunsKaizentCodexWithTheValidatedFlags() throws Exception {
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
              return new CliResult(
                  "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":10,\"output_tokens\":2}}",
                  "",
                  false,
                  Map.of("answer.txt", "{\"ok\":\"yes\"}"),
                  Duration.ofSeconds(1));
            },
            TestStructuredOutputs.create(),
            properties.timeout());
    LlmProfile profile = new LlmProfile("cli", null, "codex");

    assertThat(adapter.invalidProfile(profile)).isEmpty();
    var response =
        adapter.complete(
            new LlmRequest<>("triage", "system", "user", Answer.class, Map.of()), profile);

    assertThat(response.output()).isEqualTo(new Answer("yes"));
    assertThat(response.usage().inputTokens()).isEqualTo(10);
    assertThat(response.usage().costType()).isEqualTo(CostType.SUBSCRIPTION);
    CliCommand command = commands.getFirst();
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
            "--output-schema",
            "/scratch/schema.json",
            "--output-last-message",
            "/scratch/answer.txt",
            "-");
    assertThat(command.credentials())
        .isEqualTo(new CliCommand.Credentials("kaizent-cli-codex", "/home/kaizent/.codex-login"));
    assertThat(command.grantedPaths()).isEmpty();
    assertThat(ConfigurationPropertySources.get(environment)).isNotNull();
  }
}
