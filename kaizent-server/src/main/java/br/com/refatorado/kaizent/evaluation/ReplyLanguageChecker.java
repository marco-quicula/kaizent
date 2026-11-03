package br.com.refatorado.kaizent.evaluation;

import br.com.refatorado.kaizent.workspace.EvaluationCase.Expectation;
import com.github.pemistahl.lingua.api.IsoCode639_1;
import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Detects the language of the suggested reply itself, locally with Lingua and no model, and
 * compares it with {@code replyLanguage} (spec 0003 AC-6). A triage that detects the ticket's
 * language correctly cannot hide a reply written in another one.
 *
 * <p>For accuracy, the detector only tells apart the languages declared in the cases. With fewer
 * than two of them, it considers all the languages that Lingua supports.
 */
final class ReplyLanguageChecker implements PropertyChecker {

  private final Set<String> declared;
  private LanguageDetector detector;

  ReplyLanguageChecker(Set<String> declaredLanguages) {
    this.declared = new TreeSet<>(declaredLanguages);
  }

  @Override
  public String property() {
    return "replyLanguage";
  }

  @Override
  public CheckResult check(Expectation expect, Outcome outcome) {
    return PropertyChecker.checkProduced(
        outcome,
        outcome.suggestedReply(),
        "spec 0002",
        reply -> {
          if (isoCode(expect.replyLanguage()).isEmpty()) {
            return CheckResult.skipped(
                "Lingua cannot detect the language " + expect.replyLanguage());
          }
          Language detected = detector().detectLanguageOf(reply);
          if (detected == Language.UNKNOWN) {
            return CheckResult.fail("the reply's language could not be detected");
          }
          String code = detected.getIsoCode639_1().name().toLowerCase(Locale.ROOT);
          return code.equals(expect.replyLanguage())
              ? CheckResult.pass(code)
              : CheckResult.fail(
                  "the reply is in " + code + ", expected " + expect.replyLanguage());
        });
  }

  private synchronized LanguageDetector detector() {
    if (detector == null) {
      IsoCode639_1[] codes =
          declared.stream()
              .map(ReplyLanguageChecker::isoCode)
              .flatMap(Optional::stream)
              .toArray(IsoCode639_1[]::new);
      detector =
          codes.length >= 2
              ? LanguageDetectorBuilder.fromIsoCodes639_1(codes).build()
              : LanguageDetectorBuilder.fromAllLanguages().build();
    }
    return detector;
  }

  /** The Lingua code for an ISO 639-1 code, if Lingua supports that language. */
  private static Optional<IsoCode639_1> isoCode(String code) {
    return Arrays.stream(Language.values())
        .filter(language -> language != Language.UNKNOWN)
        .map(Language::getIsoCode639_1)
        .filter(iso -> iso.name().equalsIgnoreCase(code))
        .findFirst();
  }
}
