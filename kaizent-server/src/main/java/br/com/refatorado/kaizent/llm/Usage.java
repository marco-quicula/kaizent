package br.com.refatorado.kaizent.llm;

import java.math.BigDecimal;

/**
 * The usage of one or more model calls (spec 0004 AC-5).
 *
 * @param inputTokens input tokens, when the adapter reports them
 * @param outputTokens output tokens, when the adapter reports them
 * @param cost the cost in US dollars, when it can be computed (cost type {@code API} with a price)
 * @param costType how the call is paid for
 * @param adapter the adapter name, for example {@code gemini-api}
 * @param model the model id, if any
 */
public record Usage(
    Integer inputTokens,
    Integer outputTokens,
    BigDecimal cost,
    CostType costType,
    String adapter,
    String model) {

  /** Usage of an adapter that reports nothing. */
  public static Usage unknown(String adapter, String model) {
    return new Usage(null, null, null, CostType.UNKNOWN, adapter, model);
  }

  /** The sum of two calls of the same adapter and model, for example a call and its repair. */
  public Usage plus(Usage other) {
    return new Usage(
        sum(inputTokens, other.inputTokens),
        sum(outputTokens, other.outputTokens),
        cost == null || other.cost == null ? null : cost.add(other.cost),
        costType == other.costType ? costType : CostType.UNKNOWN,
        adapter,
        model);
  }

  /** The same usage with a cost and cost type. */
  public Usage withCost(BigDecimal newCost, CostType newCostType) {
    return new Usage(inputTokens, outputTokens, newCost, newCostType, adapter, model);
  }

  private static Integer sum(Integer a, Integer b) {
    return a == null || b == null ? null : a + b;
  }
}
