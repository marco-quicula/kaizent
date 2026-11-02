package br.com.refatorado.kaizent.llm;

import java.util.Map;

/**
 * A structured call.
 *
 * @param step the pipeline step, for example {@code resolution}; selects the profile
 * @param system the system prompt
 * @param user the user prompt
 * @param outputType the record the model output is mapped to
 * @param attributes extra data for adapters, for example {@link #TICKET_KEY}
 */
public record LlmRequest<T>(
    String step, String system, String user, Class<T> outputType, Map<String, Object> attributes) {

  /** The ticket the call is about; the fake adapter uses it to pick a script. */
  public static final String TICKET_KEY = "ticketKey";

  public LlmRequest {
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }
}
