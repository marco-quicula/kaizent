package br.com.refatorado.kaizent.llm;

/** A model call failed, or its output could not be mapped. */
public class LlmException extends RuntimeException {

  public LlmException(String message) {
    super(message);
  }

  public LlmException(String message, Throwable cause) {
    super(message, cause);
  }
}
