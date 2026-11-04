package br.com.refatorado.kaizent.llm.adapter.cli;

import br.com.refatorado.kaizent.llm.CliCommand;
import br.com.refatorado.kaizent.llm.CliExecutor;
import br.com.refatorado.kaizent.llm.CliResult;
import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmAdapter;
import br.com.refatorado.kaizent.llm.LlmException;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.llm.adapter.ModelCall;
import br.com.refatorado.kaizent.llm.adapter.StructuredOutputs;
import br.com.refatorado.kaizent.llm.adapter.cli.CliPreset.Call;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The {@code cli} adapter: a single structured call to a CLI tool, through its preset, in a new
 * container (ADR-0016, ADR-0030, spec 0004 AC-7). The call grants no workspace path; the tool runs
 * with its own read-only sandbox and without its tools, as the preset's arguments define. The
 * answer goes through {@link StructuredOutputs}, like any other adapter's.
 */
public final class CliLlmAdapter implements LlmAdapter {

  public static final String NAME = "cli";
  static final String SCHEMA_FILE = "schema.json";
  static final String PROMPT_FILE = "prompt.md";
  static final String OUTPUT_FILE = "answer.txt";

  private final Map<String, CliPreset> presets;
  private final Map<String, CliEvents> events;
  private final CliExecutor executor;
  private final StructuredOutputs structuredOutputs;
  private final Duration timeout;

  /**
   * @param presets the presets by name ({@code kaizent.cli.presets})
   * @param events the output readers; {@code text} is always available
   * @param timeout the maximum duration of one call ({@code kaizent.cli.timeout})
   */
  public CliLlmAdapter(
      Map<String, CliPreset> presets,
      List<CliEvents> events,
      CliExecutor executor,
      StructuredOutputs structuredOutputs,
      Duration timeout) {
    this.presets = Map.copyOf(presets);
    this.events =
        java.util.stream.Stream.concat(java.util.stream.Stream.of(CliEvents.TEXT), events.stream())
            .collect(
                Collectors.toUnmodifiableMap(CliEvents::name, Function.identity(), (a, b) -> b));
    this.executor = executor;
    this.structuredOutputs = structuredOutputs;
    this.timeout = timeout;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public CostType costType() {
    return CostType.SUBSCRIPTION;
  }

  @Override
  public CostType costType(LlmProfile profile) {
    CliPreset preset = profile.preset() == null ? null : presets.get(profile.preset());
    return preset == null ? costType() : preset.costType();
  }

  @Override
  public Optional<String> invalidProfile(LlmProfile profile) {
    if (profile.preset() == null || profile.preset().isBlank()) {
      return Optional.of("the cli adapter needs a preset, one of " + presets.keySet());
    }
    CliPreset preset = presets.get(profile.preset());
    if (preset == null) {
      return Optional.of(
          "unknown CLI preset " + profile.preset() + "; known presets: " + presets.keySet());
    }
    if (preset.image() == null || preset.image().isBlank()) {
      return Optional.of("CLI preset " + profile.preset() + " has no image");
    }
    if (preset.llm() == null || preset.llm().args().isEmpty()) {
      return Optional.of("CLI preset " + profile.preset() + " has no llm.args");
    }
    if (!events.containsKey(preset.llm().output().events())) {
      return Optional.of(
          "CLI preset "
              + profile.preset()
              + " reads its output with "
              + preset.llm().output().events()
              + ", one of "
              + events.keySet()
              + " is expected");
    }
    return Optional.empty();
  }

  @Override
  public <T> LlmResponse<T> complete(LlmRequest<T> request, LlmProfile profile) {
    Optional<String> invalid = invalidProfile(profile);
    if (invalid.isPresent()) {
      throw new LlmException(invalid.get());
    }
    CliPreset preset = presets.get(profile.preset());
    String schema = structuredOutputs.schema(request.outputType());
    ModelCall call = (system, user) -> call(preset, profile, schema, system, user);
    return structuredOutputs.complete(request, call);
  }

  private ModelCall.Reply call(
      CliPreset preset, LlmProfile profile, String schema, String system, String user) {
    Call llm = preset.llm();
    String prompt = system + "\n\n" + user;
    CliCommand.Builder command =
        CliCommand.builder(preset.image(), timeout)
            .args(arguments(llm, profile.model()))
            .scratchFile(SCHEMA_FILE, schema);
    if (llm.prompt() == CliPreset.PromptMode.FILE) {
      command.scratchFile(PROMPT_FILE, prompt);
    } else {
      command.stdin(prompt);
    }
    if (llm.output().source() == CliPreset.OutputSource.FILE) {
      command.outputFile(OUTPUT_FILE);
    }
    if (preset.credentials() != null) {
      command.credentials(preset.credentials().volume(), preset.credentials().path());
    }
    preset.env().forEach(command::env);
    preset.tmpfs().forEach(command::tmpfs);

    CliResult result = executor.execute(command.build());
    CliEvents reader = events.get(llm.output().events());
    String answer =
        llm.output().source() == CliPreset.OutputSource.FILE
            ? result.files().get(OUTPUT_FILE)
            : reader.answer(result.stdout()).orElse(null);
    CliEvents.Tokens tokens = reader.tokens(result.stdout());
    String model = profile.model() == null || profile.model().isBlank() ? null : profile.model();
    return new ModelCall.Reply(
        answer, new Usage(tokens.input(), tokens.output(), null, preset.costType(), NAME, model));
  }

  /** The preset's arguments, with the file paths and the model filled in. */
  static List<String> arguments(Call llm, String model) {
    List<String> args = new ArrayList<>();
    for (String arg : llm.args()) {
      args.add(fill(arg));
    }
    if (model != null && !model.isBlank()) {
      for (String arg : llm.modelArgs()) {
        args.add(fill(arg).replace(Call.MODEL, model));
      }
    }
    return args;
  }

  private static String fill(String arg) {
    return arg.replace(Call.SCHEMA_FILE, CliExecutor.SCRATCH + "/" + SCHEMA_FILE)
        .replace(Call.PROMPT_FILE, CliExecutor.SCRATCH + "/" + PROMPT_FILE)
        .replace(Call.OUTPUT_FILE, CliExecutor.SCRATCH + "/" + OUTPUT_FILE);
  }
}
