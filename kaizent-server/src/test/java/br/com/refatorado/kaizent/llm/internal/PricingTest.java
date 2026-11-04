package br.com.refatorado.kaizent.llm.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.Usage;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Spec 0004 AC-5: the cost of API calls comes from the pricing table. */
class PricingTest {

  private final Pricing pricing =
      new Pricing(
          Map.of(
              "gemini-3.6-flash",
              new LlmProperties.Price(new BigDecimal("0.75"), new BigDecimal("3.75"))));

  @Test
  void computesTheCostOfAnApiCall() {
    Usage priced = pricing.apply(api("gemini-3.6-flash", 2000, 400));

    // 2,000 × 0.75 / 1M + 400 × 3.75 / 1M = 0.0015 + 0.0015
    assertThat(priced.cost()).isEqualByComparingTo("0.003");
    assertThat(priced.costType()).isEqualTo(CostType.API);
  }

  @Test
  void givesUnknownWithoutAPriceOrWithoutTokens() {
    assertThat(pricing.apply(api("other-model", 2000, 400)))
        .satisfies(u -> assertThat(u.costType()).isEqualTo(CostType.UNKNOWN))
        .satisfies(u -> assertThat(u.cost()).isNull());
    assertThat(pricing.apply(new Usage(null, null, null, CostType.API, "a", "gemini-3.6-flash")))
        .satisfies(u -> assertThat(u.costType()).isEqualTo(CostType.UNKNOWN));
  }

  @Test
  void keepsSubscriptionAndUnknownUsageAsItIs() {
    Usage subscription = new Usage(100, 10, null, CostType.SUBSCRIPTION, "cli", "codex");

    assertThat(pricing.apply(subscription)).isEqualTo(subscription);
    assertThat(pricing.apply(Usage.unknown("fake", null))).isEqualTo(Usage.unknown("fake", null));
  }

  @Test
  void rejectsAPriceWithoutBothRates() {
    assertThatIllegalStateException()
        .isThrownBy(
            () -> new Pricing(Map.of("m", new LlmProperties.Price(null, new BigDecimal("3.75")))))
        .withMessageContaining("kaizent.llm.pricing.m needs both");
    assertThatIllegalStateException()
        .isThrownBy(() -> new Pricing(Map.of("m", new LlmProperties.Price(BigDecimal.ONE, null))));
  }

  @Test
  void tellsTheCostTypeOfAProfile() {
    assertThat(pricing.costType(CostType.API, "gemini-3.6-flash")).isEqualTo(CostType.API);
    assertThat(pricing.costType(CostType.API, "other-model")).isEqualTo(CostType.UNKNOWN);
    assertThat(pricing.costType(CostType.SUBSCRIPTION, null)).isEqualTo(CostType.SUBSCRIPTION);
  }

  private static Usage api(String model, int input, int output) {
    return new Usage(input, output, null, CostType.API, "gemini-api", model);
  }
}
