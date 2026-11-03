package br.com.refatorado.kaizent.evaluation;

import br.com.refatorado.kaizent.workspace.EvaluationCase.Expectation;
import java.util.Optional;
import java.util.function.Function;

/** Checks one expected property of an evaluation case (spec 0003 AC-6). */
interface PropertyChecker {

  /** The property name, as in the case file, for example {@code mentionsAll}. */
  String property();

  CheckResult check(Expectation expect, Outcome outcome);

  /**
   * Fails when the run produced no proposal, skips when the pipeline does not produce the value yet
   * (naming the spec that adds it), and otherwise checks the value.
   */
  static <T> CheckResult checkProduced(
      Outcome outcome, Optional<T> value, String addedBy, Function<T, CheckResult> check) {
    if (outcome.failure().isPresent()) {
      return CheckResult.fail(outcome.failure().get());
    }
    return value.map(check).orElseGet(() -> CheckResult.skipped("not produced until " + addedBy));
  }
}
