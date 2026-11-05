package br.com.refatorado.kaizent.llm.adapter.cli;

import br.com.refatorado.kaizent.llm.CostType;
import java.util.List;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How Kaizent runs one CLI tool in its container (ADR-0016, ADR-0030): {@code
 * kaizent.cli.presets.<name>}. Presets are data, not code: the bundled ones are in {@code
 * classpath:cli-presets.yaml}, and any value can be overridden in the configuration.
 *
 * @param image the tool's image, built locally from {@code docker/cli/<tool>/}
 * @param toolVersion the tool version that the preset was written for
 * @param validated whether the preset was validated with the real tool (spec 0004 AC-9)
 * @param costType how the tool's calls are paid for, usually {@code SUBSCRIPTION}
 * @param credentials the tool's login volume and where the tool keeps its login; or null
 * @param env the environment variables passed to the container, for tools that use API keys
 * @param tmpfs container paths, besides {@code /tmp}, where the tool needs to write
 * @param llm how a single structured call runs, with the tool's own tools disabled
 */
public record CliPreset(
    String image,
    String toolVersion,
    @DefaultValue("false") boolean validated,
    @DefaultValue("SUBSCRIPTION") CostType costType,
    Credentials credentials,
    @DefaultValue List<String> env,
    @DefaultValue List<String> tmpfs,
    Call llm) {

  public CliPreset {
    env = env == null ? List.of() : List.copyOf(env);
    tmpfs = tmpfs == null ? List.of() : List.copyOf(tmpfs);
  }

  /** The tool's login volume, mounted at {@code path}; Kaizent never reads it. */
  public record Credentials(String volume, String path) {}

  /**
   * One kind of call.
   *
   * @param args the command and its arguments. {@value #SCHEMA_FILE}, {@value #PROMPT_FILE} and
   *     {@value #OUTPUT_FILE} are replaced with the paths of those files in {@code /scratch}
   * @param modelArgs arguments added when the profile names a model, where {@value #MODEL} is
   *     replaced with it; empty when the tool cannot choose a model
   * @param prompt how the prompt reaches the tool
   * @param output where the answer is read and how
   */
  public record Call(
      List<String> args,
      @DefaultValue List<String> modelArgs,
      @DefaultValue("STDIN") PromptMode prompt,
      @DefaultValue Output output) {

    public static final String SCHEMA_FILE = "{schemaFile}";
    public static final String PROMPT_FILE = "{promptFile}";
    public static final String OUTPUT_FILE = "{outputFile}";
    public static final String MODEL = "{model}";

    public Call {
      args = args == null ? List.of() : List.copyOf(args);
      modelArgs = modelArgs == null ? List.of() : List.copyOf(modelArgs);
    }
  }

  /** Whether the prompt is written to the tool's standard input or to a file in /scratch. */
  public enum PromptMode {
    STDIN,
    FILE
  }

  /**
   * Where the tool's answer is.
   *
   * @param source the standard output, or the output file in /scratch
   * @param events how the standard output is read: {@code text} (the answer itself) or the name of
   *     a JSON events reader, such as {@code codex}, which also reads the token usage
   */
  public record Output(
      @DefaultValue("STDOUT") OutputSource source, @DefaultValue("text") String events) {}

  /** Where the answer is read. */
  public enum OutputSource {
    STDOUT,
    FILE
  }
}
