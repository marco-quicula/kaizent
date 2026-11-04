package br.com.refatorado.kaizent.llm.adapter.fake;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmAdapter;
import br.com.refatorado.kaizent.llm.LlmException;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmProfiles;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.llm.adapter.ModelCall;
import br.com.refatorado.kaizent.llm.adapter.StructuredOutputs;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Scripted responses for tests and offline runs (ADR-0020), with no network access. The script is
 * {@code classpath:fake/<step>/<ticket-key>.json}, or {@code fake/<step>/default.json}, and goes
 * through the same structured output mapping and repair as a model's answer. A script whose content
 * is {@value #ERROR_MARKER} makes the call fail. Spec 0004 T9 adds more markers.
 */
@Component
public class FakeLlmAdapter implements LlmAdapter {

  public static final String NAME = LlmProfiles.FAKE_ADAPTER;
  static final String ERROR_MARKER = "__error__";

  private final StructuredOutputs structuredOutputs;

  public FakeLlmAdapter(StructuredOutputs structuredOutputs) {
    this.structuredOutputs = structuredOutputs;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public CostType costType() {
    return CostType.UNKNOWN;
  }

  @Override
  public <T> LlmResponse<T> complete(LlmRequest<T> request, LlmProfile profile) {
    String script = script(request);
    if (ERROR_MARKER.equals(script.strip())) {
      throw new LlmException("Fake LLM error for step " + request.step());
    }
    return structuredOutputs.complete(
        request,
        (system, user) -> new ModelCall.Reply(script, Usage.unknown(NAME, profile.model())));
  }

  private String script(LlmRequest<?> request) {
    Object ticketKey = request.attributes().get(LlmRequest.TICKET_KEY);
    String folder = "fake/" + request.step() + "/";
    ClassPathResource specific = new ClassPathResource(folder + ticketKey + ".json");
    ClassPathResource resource =
        ticketKey != null && specific.exists()
            ? specific
            : new ClassPathResource(folder + "default.json");
    if (!resource.exists()) {
      throw new LlmException("No fake script for step " + request.step());
    }
    try {
      return resource.getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
