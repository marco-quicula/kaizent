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

  /** How a profile's calls are paid for; the CLI adapter takes it from the profile's preset. */
  default CostType costType(LlmProfile profile) {
    return costType();
  }

  /**
   * Why a profile cannot work with this adapter, for example an unknown CLI preset; empty when it
   * can. Such a profile fails at startup (spec 0004 AC-1).
   */
  default Optional<String> invalidProfile(LlmProfile profile) {
    return Optional.empty();
  }

  /**
   * Why the adapter cannot serve calls, for example a missing API key; empty when it is configured.
   * A step that uses an unconfigured adapter logs this at startup, and its calls fail with it.
   */
  default Optional<String> unavailableReason() {
    return Optional.empty();
  }
}
