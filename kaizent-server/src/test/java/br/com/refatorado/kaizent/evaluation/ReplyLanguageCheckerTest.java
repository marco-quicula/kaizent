package br.com.refatorado.kaizent.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.evaluation.CheckResult.Verdict;
import br.com.refatorado.kaizent.workspace.EvaluationCase.Expectation;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Spec 0003 AC-6: the reply language is detected from the reply itself, with Lingua. */
class ReplyLanguageCheckerTest {

  private static final String ENGLISH =
      "Hello Carla, to change an owner's address, find the owner, click Edit Owner, change the"
          + " address and save.";
  private static final String PORTUGUESE =
      "Olá Carla, para alterar o endereço de um proprietário, encontre o proprietário, clique em"
          + " Editar proprietário, altere o endereço e salve.";

  private final ReplyLanguageChecker checker = new ReplyLanguageChecker(Set.of("en", "pt"));

  @Test
  void passesWhenTheReplyIsInTheExpectedLanguage() {
    assertThat(checker.check(expecting("pt"), Outcome.reply(PORTUGUESE)))
        .isEqualTo(CheckResult.pass("pt"));
    assertThat(checker.check(expecting("en"), Outcome.reply(ENGLISH)))
        .isEqualTo(CheckResult.pass("en"));
  }

  @Test
  void failsAReplyInAnotherLanguage() {
    CheckResult result = checker.check(expecting("pt"), Outcome.reply(ENGLISH));

    assertThat(result.verdict()).isEqualTo(Verdict.FAIL);
    assertThat(result.detail()).isEqualTo("the reply is in en, expected pt");
  }

  @Test
  void worksWithASingleDeclaredLanguage() {
    ReplyLanguageChecker englishOnly = new ReplyLanguageChecker(Set.of("en"));

    assertThat(englishOnly.check(expecting("en"), Outcome.reply(PORTUGUESE)).detail())
        .isEqualTo("the reply is in pt, expected en");
  }

  private static Expectation expecting(String language) {
    return new Expectation(
        List.of("owners"), "HOW_TO", List.of(), List.of(), List.of(), false, "en", language);
  }
}
