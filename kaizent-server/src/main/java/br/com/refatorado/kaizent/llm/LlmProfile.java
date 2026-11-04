package br.com.refatorado.kaizent.llm;

/**
 * The adapter and model that serve one pipeline step ({@code kaizent.llm.profiles.<step>}).
 *
 * @param adapter the adapter name, for example {@code gemini-api} or {@code cli}
 * @param model the model id; adapters that need none ignore it. For the {@code cli} adapter it is
 *     optional and passed to the tool when its preset supports choosing a model
 * @param preset the CLI preset ({@code kaizent.cli.presets.<name>}), for the {@code cli} adapter
 *     only, for example {@code codex}
 */
public record LlmProfile(String adapter, String model, String preset) {}
