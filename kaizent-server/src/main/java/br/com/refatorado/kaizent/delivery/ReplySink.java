package br.com.refatorado.kaizent.delivery;

/** Outbound port: where approved replies go (ADR-0006, ADR-0014). */
public interface ReplySink {

  /**
   * Delivers the reply.
   *
   * @throws RuntimeException if it cannot be delivered
   */
  void send(Reply reply);
}
