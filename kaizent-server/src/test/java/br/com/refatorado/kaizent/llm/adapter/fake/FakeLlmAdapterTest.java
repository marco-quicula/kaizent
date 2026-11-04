package br.com.refatorado.kaizent.llm.adapter.fake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import br.com.refatorado.kaizent.llm.LlmException;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.llm.adapter.TestStructuredOutputs;
import br.com.refatorado.kaizent.resolution.ResolutionOutput;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FakeLlmAdapterTest {

  private final FakeLlmAdapter fake = new FakeLlmAdapter(TestStructuredOutputs.create());
  private final LlmProfile profile = new LlmProfile("fake", null, null);

  @Test
  void returnsATypedResolutionOutputFromTheDefaultScript() {
    LlmResponse<ResolutionOutput> response = fake.complete(request("PC-001"), profile);

    assertThat(response.output().diagnosis()).startsWith("Fake diagnosis");
    assertThat(response.output().suggestedReply()).isNotBlank();
    assertThat(response.output().rawConfidence()).isEqualTo(0.5);
    assertThat(response.rawOutput()).contains("\"diagnosis\"");
    assertThat(response.usage()).isEqualTo(Usage.unknown("fake", null));
  }

  @Test
  void failsWithTheErrorMarker() {
    assertThatExceptionOfType(LlmException.class)
        .isThrownBy(() -> fake.complete(request("PC-ERR"), profile))
        .withMessageContaining("Fake LLM error");
  }

  @Test
  void failsWhenTheStepHasNoScript() {
    LlmRequest<ResolutionOutput> request =
        new LlmRequest<>("unknown-step", "system", "user", ResolutionOutput.class, Map.of());

    assertThatExceptionOfType(LlmException.class)
        .isThrownBy(() -> fake.complete(request, profile))
        .withMessageContaining("No fake script");
  }

  private static LlmRequest<ResolutionOutput> request(String ticketKey) {
    return new LlmRequest<>(
        "resolution",
        "system",
        "user",
        ResolutionOutput.class,
        Map.of(LlmRequest.TICKET_KEY, ticketKey));
  }
}
