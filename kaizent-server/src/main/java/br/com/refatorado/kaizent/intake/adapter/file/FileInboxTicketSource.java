package br.com.refatorado.kaizent.intake.adapter.file;

import br.com.refatorado.kaizent.intake.IncomingTicket;
import br.com.refatorado.kaizent.intake.ProblemAttributes;
import br.com.refatorado.kaizent.intake.TicketSource;
import br.com.refatorado.kaizent.workspace.Workspace;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Reads {@code inbox/*.md} from the workspace (ADR-0028): Markdown with optional YAML front matter.
 * This is the minimal reader of spec 0002; validation and intake errors come in spec 0005, so files
 * that cannot be read are logged and skipped.
 */
@Component
public class FileInboxTicketSource implements TicketSource {

  public static final String SOURCE_ID = "file-inbox";

  private static final Logger log = LoggerFactory.getLogger(FileInboxTicketSource.class);
  private static final String FRONT_MATTER_DELIMITER = "---";

  private final Workspace workspace;

  public FileInboxTicketSource(Workspace workspace) {
    this.workspace = workspace;
  }

  @Override
  public String id() {
    return SOURCE_ID;
  }

  @Override
  public List<IncomingTicket> fetch() {
    Path inbox = workspace.inbox();
    if (!Files.isDirectory(inbox)) {
      log.debug("Inbox folder {} does not exist", inbox);
      return List.of();
    }
    List<IncomingTicket> tickets = new ArrayList<>();
    try (Stream<Path> files = Files.list(inbox)) {
      for (Path file : files.filter(FileInboxTicketSource::isTicketFile).sorted().toList()) {
        try {
          tickets.add(read(file));
        } catch (RuntimeException | IOException e) {
          log.warn("Skipping ticket file {}: {}", file.getFileName(), e.getMessage());
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot list the inbox " + inbox, e);
    }
    return tickets;
  }

  private static boolean isTicketFile(Path file) {
    return Files.isRegularFile(file) && file.getFileName().toString().endsWith(".md");
  }

  static IncomingTicket read(Path file) throws IOException {
    byte[] bytes = Files.readAllBytes(file);
    String content = new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "\n");
    Map<String, Object> frontMatter = Map.of();
    String body = content;
    if (content.startsWith(FRONT_MATTER_DELIMITER + "\n")) {
      int end = content.indexOf("\n" + FRONT_MATTER_DELIMITER, FRONT_MATTER_DELIMITER.length());
      if (end < 0) {
        throw new IllegalArgumentException("front matter is not closed with ---");
      }
      frontMatter = parseYaml(content.substring(FRONT_MATTER_DELIMITER.length() + 1, end));
      int bodyStart = content.indexOf('\n', end + 1);
      body = bodyStart < 0 ? "" : content.substring(bodyStart + 1);
    }
    body = body.strip();

    String fileName = file.getFileName().toString();
    String id = text(frontMatter.get("id"), fileName.substring(0, fileName.length() - 3));
    String title = text(frontMatter.get("title"), firstLine(body));
    Instant created =
        instant(frontMatter.get("created"), Files.getLastModifiedTime(file).toInstant());
    return new IncomingTicket(
        id,
        title,
        body,
        text(frontMatter.get("requester"), null),
        text(frontMatter.get("channel"), null),
        created,
        labels(frontMatter.get("labels")),
        sha256(bytes),
        new ProblemAttributes(Map.of("file", fileName)));
  }

  private static Map<String, Object> parseYaml(String yaml) {
    Object parsed = new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
    if (parsed == null) {
      return Map.of();
    }
    if (!(parsed instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException("front matter is not a YAML mapping");
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> values = (Map<String, Object>) map;
    return values;
  }

  private static String text(Object value, String fallback) {
    if (value == null || value.toString().isBlank()) {
      return fallback;
    }
    return value.toString().strip();
  }

  private static String firstLine(String body) {
    return body.lines()
        .map(line -> line.replaceFirst("^#+\\s*", "").strip())
        .filter(line -> !line.isEmpty())
        .findFirst()
        .orElse("(untitled)");
  }

  private static Instant instant(Object value, Instant fallback) {
    return switch (value) {
      case null -> fallback;
      case Date date -> date.toInstant();
      default -> {
        try {
          yield OffsetDateTime.parse(value.toString()).toInstant();
        } catch (DateTimeParseException e) {
          throw new IllegalArgumentException("invalid created date: " + value, e);
        }
      }
    };
  }

  private static List<String> labels(Object value) {
    return switch (value) {
      case null -> List.of();
      case List<?> list -> list.stream().map(Object::toString).map(String::strip).toList();
      default -> List.of(value.toString().strip());
    };
  }

  private static String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
