package br.com.refatorado.kaizent.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** The property checkers of spec 0003 AC-6, in the order of the case file. */
final class PropertyCheckers {

  /** Below this adjusted confidence a proposal has low confidence (spec 0003 AC-6). */
  static final double LOW_CONFIDENCE = 0.5;

  private PropertyCheckers() {}

  /**
   * All checkers.
   *
   * @param replyLanguages the languages declared in the cases, for the reply language detector
   */
  static List<PropertyChecker> all(Set<String> replyLanguages) {
    return List.of(
        subject(),
        problemType(),
        citesAny(),
        mentionsAll(),
        mentionsNone(),
        lowConfidence(),
        ticketLanguage(),
        new ReplyLanguageChecker(replyLanguages));
  }

  static PropertyChecker subject() {
    return checker(
        "subject",
        (expect, outcome) ->
            PropertyChecker.checkProduced(
                outcome,
                outcome.subject(),
                "spec 0006 (triage)",
                subject ->
                    expect.subjects().contains(subject)
                        ? CheckResult.pass(subject)
                        : CheckResult.fail(
                            "got "
                                + subject
                                + ", expected "
                                + String.join(" or ", expect.subjects()))));
  }

  static PropertyChecker problemType() {
    return checker(
        "problemType",
        (expect, outcome) ->
            PropertyChecker.checkProduced(
                outcome,
                outcome.problemType(),
                "spec 0006 (triage)",
                type ->
                    type.equals(expect.problemType())
                        ? CheckResult.pass(type)
                        : CheckResult.fail("got " + type + ", expected " + expect.problemType())));
  }

  static PropertyChecker citesAny() {
    return checker(
        "citesAny",
        (expect, outcome) ->
            PropertyChecker.checkProduced(
                outcome,
                outcome.citations(),
                "spec 0008 (resolution)",
                citations -> {
                  if (expect.citesAny().isEmpty()) {
                    return CheckResult.pass("no citation expected");
                  }
                  List<String> cited =
                      expect.citesAny().stream().filter(citations::contains).toList();
                  return cited.isEmpty()
                      ? CheckResult.fail(
                          "cites none of "
                              + String.join(", ", expect.citesAny())
                              + (citations.isEmpty()
                                  ? ""
                                  : "; cites " + String.join(", ", citations)))
                      : CheckResult.pass("cites " + String.join(", ", cited));
                }));
  }

  /** Every group must have one phrase in the reply, ignoring case. */
  static PropertyChecker mentionsAll() {
    return checker(
        "mentionsAll",
        (expect, outcome) ->
            PropertyChecker.checkProduced(
                outcome,
                outcome.suggestedReply(),
                "spec 0002",
                reply -> {
                  String text = lower(reply);
                  List<String> missing = new ArrayList<>();
                  for (List<String> group : expect.mentionsAll()) {
                    if (group.stream().noneMatch(phrase -> text.contains(lower(phrase)))) {
                      missing.add(describe(group));
                    }
                  }
                  return missing.isEmpty()
                      ? CheckResult.pass("")
                      : CheckResult.fail("missing " + String.join("; ", missing));
                }));
  }

  /** No phrase may appear in the reply, ignoring case. */
  static PropertyChecker mentionsNone() {
    return checker(
        "mentionsNone",
        (expect, outcome) ->
            PropertyChecker.checkProduced(
                outcome,
                outcome.suggestedReply(),
                "spec 0002",
                reply -> {
                  String text = lower(reply);
                  List<String> found =
                      expect.mentionsNone().stream()
                          .filter(phrase -> text.contains(lower(phrase)))
                          .map(phrase -> "\"" + phrase + "\"")
                          .toList();
                  return found.isEmpty()
                      ? CheckResult.pass("")
                      : CheckResult.fail("contains " + String.join(", ", found));
                }));
  }

  static PropertyChecker lowConfidence() {
    return checker(
        "lowConfidence",
        (expect, outcome) ->
            PropertyChecker.checkProduced(
                outcome,
                outcome.adjustedConfidence(),
                "spec 0008 (adjusted confidence)",
                confidence -> {
                  boolean low = confidence < LOW_CONFIDENCE;
                  String detail = "adjusted confidence " + confidence;
                  return low == expect.lowConfidence()
                      ? CheckResult.pass(detail)
                      : CheckResult.fail(
                          detail
                              + ", expected "
                              + (expect.lowConfidence() ? "below" : "at least")
                              + " "
                              + LOW_CONFIDENCE);
                }));
  }

  /** Checks the language that triage detected in the ticket, not the reply. */
  static PropertyChecker ticketLanguage() {
    return checker(
        "ticketLanguage",
        (expect, outcome) ->
            PropertyChecker.checkProduced(
                outcome,
                outcome.ticketLanguage(),
                "spec 0006 (triage)",
                language ->
                    language.equals(expect.ticketLanguage())
                        ? CheckResult.pass(language)
                        : CheckResult.fail(
                            "got " + language + ", expected " + expect.ticketLanguage())));
  }

  private static String describe(List<String> group) {
    return group.size() == 1
        ? "\"" + group.getFirst() + "\""
        : group.stream()
            .map(p -> "\"" + p + "\"")
            .collect(Collectors.joining(" or ", "one of ", ""));
  }

  private static String lower(String text) {
    return text.toLowerCase(Locale.ROOT);
  }

  private static PropertyChecker checker(String property, Check check) {
    return new PropertyChecker() {
      @Override
      public String property() {
        return property;
      }

      @Override
      public CheckResult check(
          br.com.refatorado.kaizent.workspace.EvaluationCase.Expectation expect, Outcome outcome) {
        return check.check(expect, outcome);
      }
    };
  }

  @FunctionalInterface
  private interface Check {
    CheckResult check(
        br.com.refatorado.kaizent.workspace.EvaluationCase.Expectation expect, Outcome outcome);
  }
}
