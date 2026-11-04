package br.com.refatorado.kaizent.llm.internal;

import br.com.refatorado.kaizent.llm.CostType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LlmGateway} used by the core: sends each request to the adapter configured for its
 * step and prices the usage. Unknown adapters fail at startup (spec 0004 AC-1).
 */
class RoutingLlmGateway implements LlmGateway {

  private static final Logger log = LoggerFactory.getLogger(RoutingLlmGateway.class);

  private final Map<String, LlmProfile> profiles;
  private final Map<String, LlmAdapter> adapters;
  private final Pricing pricing;

  RoutingLlmGateway(LlmProperties properties, List<LlmAdapter> adapters) {
    this.profiles = properties.profiles();
    this.pricing = new Pricing(properties.pricing());
    this.adapters =
        adapters.stream()
            .collect(Collectors.toUnmodifiableMap(LlmAdapter::name, Function.identity()));
    this.profiles.forEach((step, profile) -> {
      LlmAdapter adapter = profile.adapter() == null ? null : this.adapters.get(profile.adapter());
      if (adapter == null) {
        throw new IllegalStateException(
            "kaizent.llm.profiles."
                + step
                + ".adapter must be one of "
                + this.adapters.keySet()
                + ", but is "
                + profile.adapter());
      }
      adapter
          .unavailableReason()
          .ifPresent(reason ->
              log.warn(
                  "Step {} uses the {} adapter, which is not configured: {}. Its calls fail until it is.",
                  step,
                  profile.adapter(),
                  reason));
    });
  }

  @Override
  public <T> LlmResponse<T> complete(LlmRequest<T> request) {
    LlmProfile profile = profiles.get(request.step());
    if (profile == null) {
      throw new LlmException("No LLM profile for step " + request.step());
    }
    LlmResponse<T> response = adapters.get(profile.adapter()).complete(request, profile);
    return new LlmResponse<>(
        response.output(), pricing.apply(response.usage()), response.rawOutput());
  }

  /** The cost type of a profile's calls (spec 0004 AC-12). */
  CostType costType(LlmProfile profile) {
    LlmAdapter adapter = adapters.get(profile.adapter());
    return pricing.costType(adapter.costType(), profile.model());
  }
}
