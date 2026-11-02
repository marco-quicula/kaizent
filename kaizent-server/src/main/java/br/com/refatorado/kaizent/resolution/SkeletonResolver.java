package br.com.refatorado.kaizent.resolution;

import br.com.refatorado.kaizent.intake.Problem;
import br.com.refatorado.kaizent.llm.LlmGateway;
import br.com.refatorado.kaizent.llm.LlmRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * The walking-skeleton resolution: one structured {@link LlmGateway} call with a fixed default role
 * and the ticket. Spec 0008 replaces it with {@code AgentRuntime}.
 */
@Component
public class SkeletonResolver {

  static final String STEP = "resolution";
  static final String PROMPT = "prompts/skeleton-resolution.md";

  private final LlmGateway llm;
  private final String systemPrompt;

  SkeletonResolver(LlmGateway llm) {
    this.llm = llm;
    this.systemPrompt = load(PROMPT);
  }

  public Proposal resolve(Problem problem) {
    String user =
        """
        Ticket %s

        Title: %s

        %s
        """
            .formatted(problem.externalKey(), problem.title(), problem.body());
    return llm.complete(
            new LlmRequest<>(
                STEP,
                systemPrompt,
                user,
                Proposal.class,
                Map.of(LlmRequest.TICKET_KEY, problem.externalKey())))
        .output();
  }

  private static String load(String path) {
    try {
      return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the prompt " + path, e);
    }
  }
}
