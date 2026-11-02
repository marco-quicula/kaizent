package br.com.refatorado.kaizent.llm.internal;

import br.com.refatorado.kaizent.llm.LlmAdapter;
import br.com.refatorado.kaizent.llm.LlmException;
import br.com.refatorado.kaizent.llm.LlmGateway;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The {@link LlmGateway} used by the core: sends each request to the adapter configured for its
 * step. Unknown adapters fail at startup.
 */
class RoutingLlmGateway implements LlmGateway {

  private final Map<String, LlmProfile> profiles;
  private final Map<String, LlmAdapter> adapters;

  RoutingLlmGateway(LlmProperties properties, List<LlmAdapter> adapters) {
    this.profiles = properties.profiles();
    this.adapters =
        adapters.stream()
            .collect(Collectors.toUnmodifiableMap(LlmAdapter::name, Function.identity()));
    this.profiles.forEach((step, profile) -> {
      if (profile.adapter() == null || !this.adapters.containsKey(profile.adapter())) {
        throw new IllegalStateException(
            "kaizent.llm.profiles."
                + step
                + ".adapter must be one of "
                + this.adapters.keySet()
                + ", but is "
                + profile.adapter());
      }
    });
  }

  @Override
  public <T> LlmResponse<T> complete(LlmRequest<T> request) {
    LlmProfile profile = profiles.get(request.step());
    if (profile == null) {
      throw new LlmException("No LLM profile for step " + request.step());
    }
    return adapters.get(profile.adapter()).complete(request, profile);
  }
}
