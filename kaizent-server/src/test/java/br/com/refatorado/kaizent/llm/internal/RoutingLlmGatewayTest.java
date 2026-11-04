package br.com.refatorado.kaizent.llm.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.LlmAdapter;
import br.com.refatorado.kaizent.llm.LlmException;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.llm.Usage;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoutingLlmGatewayTest {

  @Test
  void sendsEachStepToItsConfiguredAdapterAndModel() {
    RoutingLlmGateway gateway =
        new RoutingLlmGateway(
            new LlmProperties(
                Map.of(
                    "resolution", new LlmProfile("a", "model-1", null),
                    "triage", new LlmProfile("b", "model-2", null)),
                Map.of(),
                null),
            List.of(new EchoAdapter("a"), new EchoAdapter("b")));

    assertThat(gateway.complete(request("resolution")).output()).isEqualTo("a:model-1");
    assertThat(gateway.complete(request("triage")).output()).isEqualTo("b:model-2");
  }

  @Test
  void pricesTheUsageOfEachCall() {
    RoutingLlmGateway gateway =
        new RoutingLlmGateway(
            new LlmProperties(
                Map.of("resolution", new LlmProfile("a", "model-1", null)),
                Map.of(
                    "model-1",
                    new LlmProperties.Price(new BigDecimal("0.75"), new BigDecimal("3.75"))),
                null),
            List.of(new EchoAdapter("a")));

    assertThat(gateway.complete(request("resolution")).usage().cost()).isEqualByComparingTo("0.75");
    assertThat(gateway.costType(new LlmProfile("a", "model-1", null))).isEqualTo(CostType.API);
    assertThat(gateway.costType(new LlmProfile("a", "model-2", null))).isEqualTo(CostType.UNKNOWN);
  }

  @Test
  void startsWithAnUnconfiguredAdapterAndLeavesTheFailureToItsCalls() {
    LlmAdapter unconfigured =
        new EchoAdapter("a") {
          @Override
          public java.util.Optional<String> unavailableReason() {
            return java.util.Optional.of("A_KEY is not set");
          }
        };

    new RoutingLlmGateway(
        new LlmProperties(Map.of("resolution", new LlmProfile("a", "m", null)), Map.of(), null),
        List.of(unconfigured));
  }

  @Test
  void failsForAStepWithoutProfile() {
    RoutingLlmGateway gateway =
        new RoutingLlmGateway(
            new LlmProperties(Map.of(), Map.of(), null), List.of(new EchoAdapter("a")));

    assertThatExceptionOfType(LlmException.class)
        .isThrownBy(() -> gateway.complete(request("resolution")))
        .withMessageContaining("No LLM profile for step resolution");
  }

  @Test
  void rejectsAnUnknownAdapterAtStartup() {
    LlmProperties properties =
        new LlmProperties(
            Map.of("resolution", new LlmProfile("missing", "m", null)), Map.of(), null);

    assertThatIllegalStateException()
        .isThrownBy(() -> new RoutingLlmGateway(properties, List.of(new EchoAdapter("a"))))
        .withMessageContaining("kaizent.llm.profiles.resolution.adapter");
  }

  private static LlmRequest<String> request(String step) {
    return new LlmRequest<>(step, "system", "user", String.class, Map.of());
  }

  private static class EchoAdapter implements LlmAdapter {
    private final String name;

    EchoAdapter(String name) {
      this.name = name;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public <T> LlmResponse<T> complete(LlmRequest<T> request, LlmProfile profile) {
      String text = name + ":" + profile.model();
      return new LlmResponse<>(
          request.outputType().cast(text),
          new Usage(1_000_000, 0, null, CostType.API, name, profile.model()),
          text);
    }

    @Override
    public CostType costType() {
      return CostType.API;
    }
  }
}
