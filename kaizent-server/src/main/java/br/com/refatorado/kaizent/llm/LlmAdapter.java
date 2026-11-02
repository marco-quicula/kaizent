package br.com.refatorado.kaizent.llm;

/**
 * An implementation of {@link LlmGateway} for one technology, registered under {@link #name()} and
 * selected per step by {@code kaizent.llm.profiles.<step>.adapter}.
 */
public interface LlmAdapter {

  /** The adapter name used in the configuration, for example {@code gemini-api} or {@code fake}. */
  String name();

  <T> LlmResponse<T> complete(LlmRequest<T> request, LlmProfile profile);
}
