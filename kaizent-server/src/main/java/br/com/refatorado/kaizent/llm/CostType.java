package br.com.refatorado.kaizent.llm;

/** How a call is paid for (ADR-0016). */
public enum CostType {
  /** Priced per token from {@code kaizent.llm.pricing}. */
  API,
  /** Covered by a subscription, with no price per call (the CLI adapters). */
  SUBSCRIPTION,
  /** Not known: no price is configured, the tokens are not reported, or a fake adapter. */
  UNKNOWN
}
