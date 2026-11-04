package br.com.refatorado.kaizent.llm.adapter;

import br.com.refatorado.kaizent.llm.Usage;

/** One raw call to a model: prompts in, text and usage out. Adapters implement it per call. */
@FunctionalInterface
public interface ModelCall {

  Reply call(String system, String user);

  /** The model's text answer and the usage of the call. */
  record Reply(String text, Usage usage) {}
}
