package fixtures.modulith.violation.orders;

import fixtures.modulith.violation.inventory.internal.StockLedger;

/** Breaks the module boundary on purpose by depending on an internal type of another module. */
public class OrderService {

  private final StockLedger ledger = new StockLedger();

  public boolean canOrder(String sku) {
    return ledger.available(sku) > 0;
  }
}
