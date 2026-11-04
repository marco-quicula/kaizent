package br.com.refatorado.kaizent.llm;

/**
 * Runs a CLI tool once, in a new container (ADR-0030). The CLI adapters of {@link LlmGateway} and
 * of the resolution module's agent runtime use it; nothing else calls a CLI tool.
 */
public interface CliExecutor {

  /** In the container, the folder that holds the granted workspace paths; the working directory. */
  String WORKSPACE = "/workspace";

  /** In the container, the writable folder of the call: input and output files, tool state. */
  String SCRATCH = "/scratch";

  /**
   * Runs the command and waits for it.
   *
   * @throws CliException when Docker is not available, the call times out (the container is then
   *     stopped), or the tool exits with a non-zero code
   */
  CliResult execute(CliCommand command);
}
