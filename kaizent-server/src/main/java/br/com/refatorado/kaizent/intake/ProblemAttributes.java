package br.com.refatorado.kaizent.intake;

import java.util.Map;

/** Connector-specific data about a problem, stored as JSON (ADR-0028). */
public record ProblemAttributes(Map<String, String> values) {

  public static final ProblemAttributes EMPTY = new ProblemAttributes(Map.of());

  public ProblemAttributes {
    values = values == null ? Map.of() : Map.copyOf(values);
  }
}
