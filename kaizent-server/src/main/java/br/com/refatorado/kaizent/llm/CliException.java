package br.com.refatorado.kaizent.llm;

/** A CLI call failed (spec 0004 AC-6). */
public class CliException extends LlmException {

  /** Why the call failed. */
  public enum Kind {
    /** The {@code docker} command is missing, or Docker could not start the container. */
    DOCKER_UNAVAILABLE,
    /** The call reached its timeout; the container was stopped. */
    TIMEOUT,
    /** The tool exited with a non-zero code. */
    NON_ZERO_EXIT,
    /** The call could not be prepared or read, for example a scratch file. */
    FAILED
  }

  private final Kind kind;
  private final Integer exitCode;

  public CliException(Kind kind, Integer exitCode, String message, Throwable cause) {
    super(message, cause);
    this.kind = kind;
    this.exitCode = exitCode;
  }

  public Kind kind() {
    return kind;
  }

  /** The exit code, for {@link Kind#NON_ZERO_EXIT} and Docker's own errors; otherwise null. */
  public Integer exitCode() {
    return exitCode;
  }
}
