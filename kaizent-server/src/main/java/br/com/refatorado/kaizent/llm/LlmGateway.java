package br.com.refatorado.kaizent.llm;

/**
 * One structured model call: a prompt in, a typed result out (ADR-0016). The adapter and model are
 * chosen by the profile of {@link LlmRequest#step()}. Never call a provider SDK from the core; use
 * this port.
 */
public interface LlmGateway {

  /**
   * Runs the call.
   *
   * @throws LlmException when the call fails or its output cannot be read as {@code outputType}
   */
  <T> LlmResponse<T> complete(LlmRequest<T> request);
}
