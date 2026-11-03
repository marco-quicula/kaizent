package br.com.refatorado.kaizent.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.evaluation.CheckResult.Verdict;
import br.com.refatorado.kaizent.workspace.EvaluationCase.Expectation;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Spec 0003 AC-6: the property checkers. */
class PropertyCheckersTest {

  private static final Expectation EXPECT =
      new Expectation(
          List.of("owners", "unknown"),
          "BUG",
          List.of("knowledge/known-issues/KI-001.md", "knowledge/faq.md"),
          List.of(List.of("10 digits", "ten digits"), List.of("Find Owners")),
          List.of("will be fixed"),
          false,
          "en",
          "en");

  @Test
  void mentionsAllNeedsOnePhraseOfEveryGroupIgnoringCase() {
    PropertyChecker checker = PropertyCheckers.mentionsAll();

    assertThat(checker.check(EXPECT, reply("Use TEN DIGITS, then click find owners.")).verdict())
        .isEqualTo(Verdict.PASS);
    CheckResult missing = checker.check(EXPECT, reply("Enter 10 digits."));
    assertThat(missing.verdict()).isEqualTo(Verdict.FAIL);
    assertThat(missing.detail()).isEqualTo("missing \"Find Owners\"");
    assertThat(checker.check(EXPECT, reply("Click Find Owners.")).detail())
        .isEqualTo("missing one of \"10 digits\" or \"ten digits\"");
  }

  @Test
  void mentionsAllComparesAccentedTextIgnoringCase() {
    Expectation portuguese =
        new Expectation(
            List.of("owners"),
            "HOW_TO",
            List.of(),
            List.of(List.of("endereço")),
            List.of(),
            false,
            "pt",
            "pt");

    assertThat(
            PropertyCheckers.mentionsAll()
                .check(portuguese, reply("Altere o ENDEREÇO e salve."))
                .verdict())
        .isEqualTo(Verdict.PASS);
  }

  @Test
  void mentionsNoneFailsOnAnyForbiddenPhrase() {
    PropertyChecker checker = PropertyCheckers.mentionsNone();

    assertThat(checker.check(EXPECT, reply("This is by design.")).verdict())
        .isEqualTo(Verdict.PASS);
    CheckResult found = checker.check(EXPECT, reply("It Will Be Fixed soon."));
    assertThat(found.verdict()).isEqualTo(Verdict.FAIL);
    assertThat(found.detail()).isEqualTo("contains \"will be fixed\"");
  }

  @Test
  void skipsWhatThePipelineDoesNotProduceYet() {
    Outcome outcome = reply("Hello");

    for (PropertyChecker checker :
        List.of(
            PropertyCheckers.subject(),
            PropertyCheckers.problemType(),
            PropertyCheckers.citesAny(),
            PropertyCheckers.lowConfidence(),
            PropertyCheckers.ticketLanguage())) {
      CheckResult result = checker.check(EXPECT, outcome);
      assertThat(result.verdict()).as(checker.property()).isEqualTo(Verdict.SKIPPED);
      assertThat(result.detail()).as(checker.property()).startsWith("not produced until spec 000");
    }
  }

  @Test
  void checksTriageCitationsAndConfidenceOnceTheyAreProduced() {
    Outcome outcome =
        new Outcome(
            Optional.of("Hello"),
            Optional.of("unknown"),
            Optional.of("HOW_TO"),
            Optional.of(List.of("knowledge/faq.md")),
            Optional.of(0.4),
            Optional.of("pt"),
            Optional.empty());

    assertThat(PropertyCheckers.subject().check(EXPECT, outcome).verdict()).isEqualTo(Verdict.PASS);
    assertThat(PropertyCheckers.problemType().check(EXPECT, outcome).detail())
        .isEqualTo("got HOW_TO, expected BUG");
    assertThat(PropertyCheckers.citesAny().check(EXPECT, outcome).verdict())
        .isEqualTo(Verdict.PASS);
    assertThat(PropertyCheckers.lowConfidence().check(EXPECT, outcome).detail())
        .isEqualTo("adjusted confidence 0.4, expected at least 0.5");
    assertThat(PropertyCheckers.ticketLanguage().check(EXPECT, outcome).verdict())
        .isEqualTo(Verdict.FAIL);
  }

  @Test
  void citesAnyFailsWithoutAnExpectedCitationAndPassesWhenNoneIsExpected() {
    Outcome outcome =
        new Outcome(
            Optional.of("Hello"),
            Optional.empty(),
            Optional.empty(),
            Optional.of(List.of("knowledge/product/owners.md")),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    Expectation noCitation =
        new Expectation(
            List.of("owners"), "OTHER", List.of(), List.of(), List.of(), true, "en", "en");

    assertThat(PropertyCheckers.citesAny().check(EXPECT, outcome).verdict())
        .isEqualTo(Verdict.FAIL);
    assertThat(PropertyCheckers.citesAny().check(noCitation, outcome).verdict())
        .isEqualTo(Verdict.PASS);
  }

  @Test
  void failsEveryPropertyOfARunWithoutAProposal() {
    Outcome failed = Outcome.failed("the run is FAILED: Fake LLM error");

    for (PropertyChecker checker : PropertyCheckers.all(java.util.Set.of("en", "pt"))) {
      CheckResult result = checker.check(EXPECT, failed);
      assertThat(result.verdict()).as(checker.property()).isEqualTo(Verdict.FAIL);
      assertThat(result.detail()).isEqualTo("the run is FAILED: Fake LLM error");
    }
  }

  @Test
  void checksTheEightPropertiesOfACaseInOrder() {
    assertThat(PropertyCheckers.all(java.util.Set.of("en")))
        .extracting(PropertyChecker::property)
        .containsExactly(
            "subject",
            "problemType",
            "citesAny",
            "mentionsAll",
            "mentionsNone",
            "lowConfidence",
            "ticketLanguage",
            "replyLanguage");
  }

  private static Outcome reply(String text) {
    return Outcome.reply(text);
  }
}
