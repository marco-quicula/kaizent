/**
 * LLM access (ADR-0016): the {@link br.com.refatorado.kaizent.llm.LlmGateway} port for single
 * structured calls, and its adapters, chosen per pipeline step by {@code
 * kaizent.llm.profiles.<step>.adapter}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "LLM")
package br.com.refatorado.kaizent.llm;
