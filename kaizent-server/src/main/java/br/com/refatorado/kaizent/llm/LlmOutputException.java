package br.com.refatorado.kaizent.llm;

/**
 * The model's output could not be mapped to the requested type, even after the one repair attempt
 * (spec 0004 AC-2).
 */
public class LlmOutputException extends LlmException {

  private final String rawOutput;

  public LlmOutputException(String message, String rawOutput) {
    super(message);
    this.rawOutput = rawOutput;
  }

  /** The last output received from the model. */
  public String rawOutput() {
    return rawOutput;
  }
}
