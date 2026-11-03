package br.com.refatorado.kaizent.evaluation;

/**
 * The result of checking one property of one case.
 *
 * @param detail why it failed or was skipped, or what was found; empty when there is nothing to add
 */
record CheckResult(Verdict verdict, String detail) {

  enum Verdict {
    PASS,
    FAIL,
    /** The pipeline does not produce the property yet, or it cannot be checked. */
    SKIPPED
  }

  static CheckResult pass(String detail) {
    return new CheckResult(Verdict.PASS, detail);
  }

  static CheckResult fail(String detail) {
    return new CheckResult(Verdict.FAIL, detail);
  }

  static CheckResult skipped(String detail) {
    return new CheckResult(Verdict.SKIPPED, detail);
  }
}
