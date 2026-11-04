package br.com.refatorado.kaizent.llm.internal;

import br.com.refatorado.kaizent.llm.CostType;
import br.com.refatorado.kaizent.llm.Usage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Prices the usage of API calls from {@code kaizent.llm.pricing} (spec 0004 AC-5). A call without a
 * price or without token counts gets cost type {@code UNKNOWN}; other cost types are kept as they
 * are.
 */
final class Pricing {

  private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

  private final Map<String, LlmProperties.Price> prices;

  Pricing(Map<String, LlmProperties.Price> prices) {
    this.prices = prices;
  }

  Usage apply(Usage usage) {
    if (usage.costType() != CostType.API) {
      return usage;
    }
    LlmProperties.Price price = usage.model() == null ? null : prices.get(usage.model());
    if (price == null
        || price.inputPerMillion() == null
        || price.outputPerMillion() == null
        || usage.inputTokens() == null
        || usage.outputTokens() == null) {
      return usage.withCost(null, CostType.UNKNOWN);
    }
    BigDecimal cost =
        price
            .inputPerMillion()
            .multiply(BigDecimal.valueOf(usage.inputTokens()))
            .add(price.outputPerMillion().multiply(BigDecimal.valueOf(usage.outputTokens())))
            .divide(MILLION, 8, RoundingMode.HALF_UP)
            .stripTrailingZeros();
    return usage.withCost(cost, CostType.API);
  }

  /** The cost type that a profile's calls get: {@code UNKNOWN} for an API model without price. */
  CostType costType(CostType adapterCostType, String model) {
    if (adapterCostType != CostType.API) {
      return adapterCostType;
    }
    return model != null && prices.containsKey(model) ? CostType.API : CostType.UNKNOWN;
  }
}
