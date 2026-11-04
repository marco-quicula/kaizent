package br.com.refatorado.kaizent.llm;

import java.util.Optional;

/**
 * An implementation of {@link LlmGateway} for one technology, registered under {@link #name()} and
 * selected per step by {@code kaizent.llm.profiles.<step>.adapter}.
 */
public interface LlmAdapter {

  /** The adapter name used in the configuration, for example {@code gemini-api} or {@code fake}. */
  String name();

  /**
   * Runs the call. The returned usage carries the tokens; the gateway prices it from {@code
   * kaizent.llm.pricing}.
   */
  <T> LlmResponse<T> complete(LlmRequest<T> request, LlmProfile profile);

  /** How this adapter's calls are paid for. */
  CostType costType();

  /**
   * Why the adapter cannot serve calls, for example a missing API key; empty when it is configured.
   * A step that uses an unconfigured adapter logs this at startup, and its calls fail with it.
   */
  default Optional<String> unavailableReason() {
    return Optional.empty();
  }
}
