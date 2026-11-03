package br.com.refatorado.kaizent.evaluation;

import br.com.refatorado.kaizent.run.Run;
import br.com.refatorado.kaizent.run.RunStatus;
import java.util.List;
import java.util.Optional;

/**
 * What an evaluation run produced, as the property checkers see it. A property that the pipeline
 * does not produce yet is empty: triage arrives in spec 0006, citations and the adjusted confidence
 * in spec 0008.
 *
 * @param failure why the run produced no proposal, if it did not
 */
record Outcome(
    Optional<String> suggestedReply,
    Optional<String> subject,
    Optional<String> problemType,
    Optional<List<String>> citations,
    Optional<Double> adjustedConfidence,
    Optional<String> ticketLanguage,
    Optional<String> failure) {

  static Outcome of(Run run) {
    if (run.status() != RunStatus.EVALUATED || run.proposal() == null) {
      String reason =
          run.failureMessage() != null
              ? "the run is " + run.status() + ": " + run.failureMessage()
              : "the run is " + run.status();
      return failed(reason);
    }
    return reply(run.proposal().suggestedReply());
  }

  /** A proposal's reply; a missing or blank reply is a failure of the case, not a skip. */
  static Outcome reply(String suggestedReply) {
    if (suggestedReply == null || suggestedReply.isBlank()) {
      return failed("the proposal has no suggested reply");
    }
    return new Outcome(
        Optional.of(suggestedReply),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  static Outcome failed(String reason) {
    return new Outcome(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(reason));
  }
}
