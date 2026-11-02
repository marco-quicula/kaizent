package br.com.refatorado.kaizent.llm;

/**
 * The result of a structured call. Spec 0004 adds the token usage and cost.
 *
 * @param output the mapped result
 * @param rawOutput the model's text output, as received
 */
public record LlmResponse<T>(T output, String rawOutput) {}
