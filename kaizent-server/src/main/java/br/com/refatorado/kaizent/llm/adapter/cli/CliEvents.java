package br.com.refatorado.kaizent.llm.adapter.cli;

import java.util.Optional;

/**
 * Reads a tool's standard output (spec 0004 AC-7): the final answer, when the tool does not write
 * it to a file, and the token usage, when the tool reports it. One reader per output format,
 * selected by the preset's {@code output.events}.
 */
public interface CliEvents {

  /** The reader's name in presets, for example {@code text} or {@code codex}. */
  String name();

  /** The final answer in the standard output, if any. */
  Optional<String> answer(String stdout);

  /** The tokens that the tool reports, or unknown counts. */
  Tokens tokens(String stdout);

  /** Input and output tokens; null when the tool does not report them. */
  record Tokens(Integer input, Integer output) {
    public static final Tokens UNKNOWN = new Tokens(null, null);
  }

  /** The standard output is the answer; no usage is reported. */
  CliEvents TEXT =
      new CliEvents() {
        @Override
        public String name() {
          return "text";
        }

        @Override
        public Optional<String> answer(String stdout) {
          return stdout == null || stdout.isBlank() ? Optional.empty() : Optional.of(stdout);
        }

        @Override
        public Tokens tokens(String stdout) {
          return Tokens.UNKNOWN;
        }
      };
}
