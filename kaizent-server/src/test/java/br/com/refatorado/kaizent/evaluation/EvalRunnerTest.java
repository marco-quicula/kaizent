package br.com.refatorado.kaizent.evaluation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import br.com.refatorado.kaizent.llm.LlmProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Spec 0003 AC-5: the runner refuses the fake adapters unless it is a dry run. */
class EvalRunnerTest {

  private static final Map<String, LlmProfile> FAKE =
      Map.of(
          "resolution", new LlmProfile("fake", null, null),
          "triage", new LlmProfile("gemini-api", "gemini-3.6-flash", null));

  @Test
  void refusesTheFakeAdapters() {
    assertThatIllegalStateException()
        .isThrownBy(() -> EvalRunner.refuseFakeAdapters(FAKE, false))
        .withMessageStartingWith(
            "Evaluation needs real models, but these steps use the fake adapter: resolution.")
        .withMessageContaining("--dry-run");
  }

  @Test
  void acceptsTheFakeAdaptersInADryRun() {
    assertThatCode(() -> EvalRunner.refuseFakeAdapters(FAKE, true)).doesNotThrowAnyException();
  }

  @Test
  void acceptsRealAdapters() {
    assertThatCode(() ->
            EvalRunner.refuseFakeAdapters(
                Map.of("resolution", new LlmProfile("gemini-api", "gemini-3.6-flash", null)),
                false))
        .doesNotThrowAnyException();
  }
}
