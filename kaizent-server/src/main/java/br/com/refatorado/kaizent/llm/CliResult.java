package br.com.refatorado.kaizent.llm;

import java.time.Duration;
import java.util.Map;

/**
 * The result of a CLI call that exited with code 0.
 *
 * @param stdout the standard output, cut at {@code kaizent.cli.max-output}
 * @param stderr the standard error, cut at {@code kaizent.cli.max-output}
 * @param truncated whether the standard output, the standard error or an output file was cut
 * @param files the requested output files that the tool wrote, by file name
 * @param duration how long the call took, container start included
 */
public record CliResult(
    String stdout, String stderr, boolean truncated, Map<String, String> files, Duration duration) {

  public CliResult {
    files = Map.copyOf(files);
  }
}
