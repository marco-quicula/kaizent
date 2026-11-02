package br.com.refatorado.kaizent.review;

import br.com.refatorado.kaizent.run.VerdictType;

/** A verdict type that this version does not accept yet. */
public class UnsupportedVerdictException extends RuntimeException {

  public UnsupportedVerdictException(VerdictType type) {
    super("Verdict " + type + " is not supported yet; only APPROVED is");
  }
}
