package br.com.refatorado.kaizent.llm;

/**
 * The adapter and model that serve one pipeline step ({@code kaizent.llm.profiles.<step>}).
 *
 * @param adapter the adapter name, for example {@code gemini-api}
 * @param model the model id; adapters that need none ignore it
 */
public record LlmProfile(String adapter, String model) {}
