package br.com.refatorado.kaizent.llm;

import java.time.Duration;

/**
 * A model call failed at the provider (spec 0004 AC-4). It keeps the provider's cause, so that the
 * run's failure and the evaluation report show it instead of a generic error.
 */
public class LlmCallException extends LlmException {

  private final Integer status;
  private final String providerCode;
  private final String providerMessage;
  private final Duration retryAfter;
  private final boolean retryable;

  /**
   * @param provider the provider's name for messages, for example {@code Gemini API}
   * @param status the HTTP status, if any
   * @param providerCode the provider's error code, for example {@code RESOURCE_EXHAUSTED}
   * @param providerMessage the provider's error message
   * @param retryAfter the wait that the provider suggests, if any
   * @param retryable whether the error is transient: HTTP 429, 5xx or a network error
   */
  public LlmCallException(
      String provider,
      Integer status,
      String providerCode,
      String providerMessage,
      Duration retryAfter,
      boolean retryable,
      Throwable cause) {
    super(describe(provider, status, providerCode, providerMessage, retryAfter), cause);
    this.status = status;
    this.providerCode = providerCode;
    this.providerMessage = providerMessage;
    this.retryAfter = retryAfter;
    this.retryable = retryable;
  }

  /** The same error, with a note on how retrying ended, for example after three attempts. */
  public LlmCallException withNote(String note) {
    return new LlmCallException(getMessage() + " (" + note + ")", this);
  }

  private LlmCallException(String message, LlmCallException original) {
    super(message, original.getCause());
    this.status = original.status;
    this.providerCode = original.providerCode;
    this.providerMessage = original.providerMessage;
    this.retryAfter = original.retryAfter;
    this.retryable = original.retryable;
  }

  public Integer status() {
    return status;
  }

  public String providerCode() {
    return providerCode;
  }

  public String providerMessage() {
    return providerMessage;
  }

  public Duration retryAfter() {
    return retryAfter;
  }

  public boolean retryable() {
    return retryable;
  }

  private static String describe(
      String provider, Integer status, String code, String message, Duration retryAfter) {
    StringBuilder text = new StringBuilder(provider).append(" call failed:");
    if (status != null) {
      text.append(" HTTP ").append(status);
    }
    if (code != null && !code.isBlank()) {
      text.append(' ').append(code);
    }
    if (message != null && !message.isBlank()) {
      text.append(status != null || code != null ? ": " : " ").append(message);
    }
    if (retryAfter != null) {
      text.append(" (the provider asks to wait ").append(retryAfter.toSeconds()).append("s)");
    }
    return text.toString();
  }
}
