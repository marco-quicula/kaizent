package br.com.refatorado.kaizent.llm;

/**
 * The result of a structured call.
 *
 * @param output the mapped and validated result
 * @param usage the usage of the call, including a repair attempt, if any
 * @param rawOutput the model's last text output, as received
 */
public record LlmResponse<T>(T output, Usage usage, String rawOutput) {}
