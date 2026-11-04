package br.com.refatorado.kaizent.llm.adapter;

import br.com.refatorado.kaizent.llm.LlmCallException;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.client.RestClientResponseException;

/**
 * Turns a provider SDK's exception into an {@link LlmCallException} that keeps the HTTP status, the
 * provider's error code and message, and the suggested wait (spec 0004 AC-4). It understands the
 * Google GenAI, OpenAI and Anthropic SDKs and Spring's {@code RestClient} (Ollama), looking through
 * the whole cause chain, since Spring AI may wrap them.
 */
public final class ProviderErrors {

  private static final int MAX_MESSAGE = 500;
  private static final Pattern RETRY_DELAY =
      Pattern.compile(
          "\"retryDelay\"\\s*:\\s*\"(\\d+(?:\\.\\d+)?)s\"|retry in (\\d+(?:\\.\\d+)?)\\s*s");

  private ProviderErrors() {}

  private static final String REDACTED = "[REDACTED]";

  /**
   * Describes a failed call, with the given credentials removed from its message and from its
   * causes (spec 0004 AC-11): a provider may echo a key in an error message.
   */
  public static LlmCallException describe(
      String provider, Throwable error, Collection<String> secrets) {
    List<String> present =
        secrets.stream().filter(secret -> secret != null && !secret.isBlank()).toList();
    // The texts are redacted before they are shortened, so a cut never leaves part of a key.
    LlmCallException described = describe(provider, error, present);
    if (present.isEmpty() || !mentions(error, present)) {
      return described;
    }
    return new LlmCallException(
        provider,
        described.status(),
        described.providerCode(),
        described.providerMessage(),
        described.retryAfter(),
        described.retryable(),
        redacted(error, present));
  }

  /**
   * Describes a failed call; {@code provider} names it in the message, for example "OpenAI API".
   */
  public static LlmCallException describe(String provider, Throwable error) {
    return describe(provider, error, List.of());
  }

  private static LlmCallException describe(String provider, Throwable error, List<String> secrets) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      LlmCallException described = describeOne(provider, cause, error, secrets);
      if (described != null) {
        return described;
      }
      if (cause.getCause() == cause) {
        break;
      }
    }
    return new LlmCallException(
        provider, null, null, shorten(rootMessage(error), secrets), null, false, error);
  }

  private static LlmCallException describeOne(
      String provider, Throwable cause, Throwable error, List<String> secrets) {
    if (cause instanceof LlmCallException known) {
      return known;
    }
    if (cause instanceof com.google.genai.errors.ApiException api) {
      String message = api.message();
      return call(provider, api.code(), api.status(), message, retryDelay(message), error, secrets);
    }
    if (cause instanceof com.openai.errors.OpenAIServiceException openAi) {
      return call(
          provider,
          openAi.statusCode(),
          openAi.code().orElse(openAi.type().orElse(null)),
          openAi.getMessage(),
          retryAfter(
              openAi.headers().values("retry-after"), openAi.headers().values("retry-after-ms")),
          error,
          secrets);
    }
    if (cause instanceof com.anthropic.errors.AnthropicServiceException anthropic) {
      return call(
          provider,
          anthropic.statusCode(),
          null,
          anthropic.getMessage(),
          retryAfter(anthropic.headers().values("retry-after"), List.of()),
          error,
          secrets);
    }
    if (cause instanceof RestClientResponseException rest) {
      String retry =
          rest.getResponseHeaders() == null
              ? null
              : rest.getResponseHeaders().getFirst("Retry-After");
      String body = rest.getResponseBodyAsString();
      return call(
          provider,
          rest.getStatusCode().value(),
          null,
          body.isBlank() ? rest.getStatusText() : body,
          retryAfter(retry == null ? List.of() : List.of(retry), List.of()),
          error,
          secrets);
    }
    if (cause instanceof IOException
        || cause instanceof com.openai.errors.OpenAIIoException
        || cause instanceof com.anthropic.errors.AnthropicIoException) {
      // A network error before any answer: transient.
      return new LlmCallException(
          provider, null, null, shorten(rootMessage(cause), secrets), null, true, error);
    }
    return null;
  }

  private static LlmCallException call(
      String provider,
      int status,
      String code,
      String message,
      Duration retryAfter,
      Throwable error,
      List<String> secrets) {
    boolean retryable = status == 429 || status >= 500;
    return new LlmCallException(
        provider,
        status,
        shorten(code, secrets),
        shorten(message, secrets),
        retryAfter,
        retryable,
        error);
  }

  /** Whether a message in the cause chain holds a secret. */
  private static boolean mentions(Throwable error, List<String> secrets) {
    for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
      String message = t.getMessage();
      if (message != null && secrets.stream().anyMatch(message::contains)) {
        return true;
      }
    }
    return false;
  }

  static String redact(String text, List<String> secrets) {
    if (text == null) {
      return null;
    }
    String result = text;
    for (String secret : secrets) {
      result = result.replace(secret, REDACTED);
    }
    return result;
  }

  /** A copy of the cause chain with the secrets removed from the messages. */
  private static Throwable redacted(Throwable error, List<String> secrets) {
    if (error == null) {
      return null;
    }
    Throwable cause = error.getCause() == error ? null : redacted(error.getCause(), secrets);
    return new RedactedException(error, redact(error.getMessage(), secrets), cause);
  }

  /** Stands for a provider exception whose message held a credential. */
  static final class RedactedException extends RuntimeException {
    RedactedException(Throwable original, String message, Throwable cause) {
      super(original.getClass().getName() + (message == null ? "" : ": " + message), cause);
      setStackTrace(original.getStackTrace());
    }
  }

  /** {@code Retry-After} in seconds or as an HTTP date, or {@code retry-after-ms}. */
  static Duration retryAfter(List<String> retryAfter, List<String> retryAfterMs) {
    if (!retryAfterMs.isEmpty()) {
      try {
        return Duration.ofMillis(Math.round(Double.parseDouble(retryAfterMs.getFirst().strip())));
      } catch (NumberFormatException e) {
        // Fall back to Retry-After.
      }
    }
    if (retryAfter.isEmpty()) {
      return null;
    }
    String value = retryAfter.getFirst().strip();
    try {
      return Duration.ofMillis(Math.round(Double.parseDouble(value) * 1000));
    } catch (NumberFormatException e) {
      try {
        Duration until =
            Duration.between(
                ZonedDateTime.now(),
                ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME));
        return until.isNegative() ? Duration.ZERO : until;
      } catch (DateTimeParseException ignored) {
        return null;
      }
    }
  }

  /** The {@code retryDelay} of a Gemini {@code RetryInfo}, or "retry in 12.3s" in its message. */
  static Duration retryDelay(String message) {
    if (message == null) {
      return null;
    }
    Matcher matcher = RETRY_DELAY.matcher(message);
    if (!matcher.find()) {
      return null;
    }
    String seconds = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    return Duration.ofMillis(Math.round(Double.parseDouble(seconds) * 1000));
  }

  private static String rootMessage(Throwable error) {
    Throwable root = error;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
  }

  /** Redacts the secrets first, then puts the message on one line and cuts it. */
  private static String shorten(String message, List<String> secrets) {
    if (message == null) {
      return null;
    }
    String oneLine = redact(message, secrets).strip().replaceAll("\\s+", " ");
    return oneLine.length() <= MAX_MESSAGE ? oneLine : oneLine.substring(0, MAX_MESSAGE) + "…";
  }
}
