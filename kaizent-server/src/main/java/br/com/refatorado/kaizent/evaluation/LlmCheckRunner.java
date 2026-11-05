package br.com.refatorado.kaizent.evaluation;

import br.com.refatorado.kaizent.llm.LlmGateway;
import br.com.refatorado.kaizent.llm.LlmProfile;
import br.com.refatorado.kaizent.llm.LlmProfiles;
import br.com.refatorado.kaizent.llm.LlmRequest;
import br.com.refatorado.kaizent.llm.LlmResponse;
import br.com.refatorado.kaizent.llm.Usage;
import br.com.refatorado.kaizent.workspace.Workspace;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * The {@code llm-check} command (spec 0004 T13): one triage-shaped structured call on one ticket of
 * the workspace's inbox, through the profile of {@code kaizent.llm-check.step}, with any adapter:
 * API, CLI preset or fake. It prints the answer, the usage and the duration, and exits with code 0,
 * or 1 when the call fails. It stores nothing and delivers nothing; it checks a configuration
 * before running the pipeline, and runs the manual validations EV-1 to EV-3.
 */
@Component
@Profile("llm-check")
@EnableConfigurationProperties(LlmCheckProperties.class)
class LlmCheckRunner implements ApplicationRunner {

  static final String PROMPT = "prompts/llm-check-triage.md";
  private static final Pattern KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]*");

  private final LlmGateway llm;
  private final LlmProfiles profiles;
  private final Workspace workspace;
  private final LlmCheckProperties properties;
  private final Clock clock;
  private final ConfigurableApplicationContext context;
  private final PrintStream out;

  @Autowired
  LlmCheckRunner(
      LlmGateway llm,
      LlmProfiles profiles,
      Workspace workspace,
      LlmCheckProperties properties,
      Clock clock,
      ConfigurableApplicationContext context) {
    this(llm, profiles, workspace, properties, clock, context, System.out);
  }

  LlmCheckRunner(
      LlmGateway llm,
      LlmProfiles profiles,
      Workspace workspace,
      LlmCheckProperties properties,
      Clock clock,
      ConfigurableApplicationContext context,
      PrintStream out) {
    this.llm = llm;
    this.profiles = profiles;
    this.workspace = workspace;
    this.properties = properties;
    this.clock = clock;
    this.context = context;
    this.out = out;
  }

  @Override
  public void run(ApplicationArguments args) {
    int code = check();
    if (properties.exitWhenDone()) {
      System.exit(SpringApplication.exit(context, () -> code));
    }
  }

  /** Runs the check and prints its result; returns the exit code. */
  int check() {
    String ticket = properties.ticket();
    if (ticket == null || !KEY.matcher(ticket).matches()) {
      out.println("llm-check: set --kaizent.llm-check.ticket to a ticket key, for example PC-002");
      return 1;
    }
    LlmProfile profile = profiles.byStep().get(properties.step());
    if (profile == null) {
      out.println(
          "llm-check: no profile for step "
              + properties.step()
              + "; configured steps: "
              + profiles.byStep().keySet());
      return 1;
    }
    Path file = workspace.inbox().resolve(ticket + ".md");
    if (!Files.isRegularFile(file)) {
      out.println("llm-check: ticket " + ticket + " not found at " + file);
      return 1;
    }
    out.println(
        "llm-check: ticket "
            + ticket
            + ", step "
            + properties.step()
            + ", adapter "
            + profile.adapter()
            + (profile.preset() == null || profile.preset().isBlank()
                ? ""
                : ", preset " + profile.preset())
            + (profile.model() == null || profile.model().isBlank()
                ? ""
                : ", model " + profile.model()));
    Instant start = clock.instant();
    try {
      LlmResponse<TriageCheck> response =
          llm.complete(
              new LlmRequest<>(
                  properties.step(),
                  read(new ClassPathResource(PROMPT)),
                  "Ticket " + ticket + ":\n\n" + Files.readString(file, StandardCharsets.UTF_8),
                  TriageCheck.class,
                  Map.of(LlmRequest.TICKET_KEY, ticket)));
      Duration took = Duration.between(start, clock.instant());
      TriageCheck answer = response.output();
      out.println("llm-check: OK in " + seconds(took));
      out.println("  category:   " + answer.category());
      out.println("  subject:    " + answer.subject());
      out.println("  language:   " + answer.language());
      out.println("  summary:    " + answer.summary());
      out.println("  confidence: " + answer.confidence());
      out.println("  usage:      " + usage(response.usage()));
      return 0;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (RuntimeException e) {
      Duration took = Duration.between(start, clock.instant());
      out.println("llm-check: FAILED after " + seconds(took) + ": " + e.getMessage());
      return 1;
    }
  }

  private static String usage(Usage usage) {
    return "input tokens "
        + (usage.inputTokens() == null ? "unknown" : usage.inputTokens())
        + ", output tokens "
        + (usage.outputTokens() == null ? "unknown" : usage.outputTokens())
        + ", cost "
        + (usage.cost() == null ? "unknown" : "USD " + usage.cost().toPlainString())
        + ", cost type "
        + usage.costType()
        + ", adapter "
        + usage.adapter()
        + (usage.model() == null ? "" : ", model " + usage.model());
  }

  private static String seconds(Duration duration) {
    return String.format(java.util.Locale.ROOT, "%.1f s", duration.toMillis() / 1000.0);
  }

  private static String read(ClassPathResource resource) throws IOException {
    return resource.getContentAsString(StandardCharsets.UTF_8);
  }
}
