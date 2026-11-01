package fixtures.modulith.violation.inventory.internal;

/** An internal type of the {@code inventory} module; other modules must not use it. */
public class StockLedger {

  public int available(String sku) {
    return 0;
  }
}
