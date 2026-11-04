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
                    "resolution", new LlmProfile("a", "model-1"),
                    "triage", new LlmProfile("b", "model-2")),
                Map.of()),
            List.of(new EchoAdapter("a"), new EchoAdapter("b")));

    assertThat(gateway.complete(request("resolution")).output()).isEqualTo("a:model-1");
    assertThat(gateway.complete(request("triage")).output()).isEqualTo("b:model-2");
  }

  @Test
  void pricesTheUsageOfEachCall() {
    RoutingLlmGateway gateway =
        new RoutingLlmGateway(
            new LlmProperties(
                Map.of("resolution", new LlmProfile("a", "model-1")),
                Map.of(
                    "model-1",
                    new LlmProperties.Price(new BigDecimal("0.75"), new BigDecimal("3.75")))),
            List.of(new EchoAdapter("a")));

    assertThat(gateway.complete(request("resolution")).usage().cost()).isEqualByComparingTo("0.75");
    assertThat(gateway.costType(new LlmProfile("a", "model-1"))).isEqualTo(CostType.API);
    assertThat(gateway.costType(new LlmProfile("a", "model-2"))).isEqualTo(CostType.UNKNOWN);
  }

  @Test
  void failsForAStepWithoutProfile() {
    RoutingLlmGateway gateway =
        new RoutingLlmGateway(new LlmProperties(Map.of(), Map.of()), List.of(new EchoAdapter("a")));

    assertThatExceptionOfType(LlmException.class)
        .isThrownBy(() -> gateway.complete(request("resolution")))
        .withMessageContaining("No LLM profile for step resolution");
  }

  @Test
  void rejectsAnUnknownAdapterAtStartup() {
    LlmProperties properties =
        new LlmProperties(Map.of("resolution", new LlmProfile("missing", "m")), Map.of());

    assertThatIllegalStateException()
        .isThrownBy(() -> new RoutingLlmGateway(properties, List.of(new EchoAdapter("a"))))
        .withMessageContaining("kaizent.llm.profiles.resolution.adapter");
  }

  private static LlmRequest<String> request(String step) {
    return new LlmRequest<>(step, "system", "user", String.class, Map.of());
  }

  private record EchoAdapter(String name) implements LlmAdapter {
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
