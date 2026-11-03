package br.com.refatorado.kaizent.workspace;

import java.util.Map;
import java.util.Optional;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Parses workspace YAML files and the YAML front matter of Markdown files, with SnakeYAML's safe
 * constructor. Intake and the workspace loader share it, so that both read a file the same way.
 */
public final class YamlDocument {

  private static final String DELIMITER = "---";
  private static final char BYTE_ORDER_MARK = '﻿';

  private YamlDocument() {}

  /**
   * A Markdown file split into its front matter and its body.
   *
   * @param body the text after the closing {@code ---} line, without surrounding blank space
   */
  public record FrontMatter(Map<String, Object> fields, String body) {}

  /** Removes a leading byte order mark and turns CRLF line endings into LF. */
  public static String normalize(String text) {
    String normalized = text.replace("\r\n", "\n");
    return !normalized.isEmpty() && normalized.charAt(0) == BYTE_ORDER_MARK
        ? normalized.substring(1)
        : normalized;
  }

  /**
   * Parses a YAML mapping.
   *
   * @throws IllegalArgumentException if the text is not valid YAML or not a mapping
   */
  public static Map<String, Object> mapping(String yaml) {
    Object parsed;
    try {
      parsed = new Yaml(new SafeConstructor(new LoaderOptions())).load(normalize(yaml));
    } catch (YAMLException e) {
      throw new IllegalArgumentException("invalid YAML: " + firstLine(e.getMessage()), e);
    }
    if (parsed == null) {
      return Map.of();
    }
    if (!(parsed instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException("not a YAML mapping");
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> values = (Map<String, Object>) map;
    return values;
  }

  /**
   * Splits Markdown into its front matter and its body. The front matter starts with a {@code ---}
   * first line and ends with the next line that is exactly {@code ---}.
   *
   * @return empty if the file does not start with a {@code ---} line
   * @throws IllegalArgumentException if the front matter is not closed or not a valid mapping
   */
  public static Optional<FrontMatter> frontMatter(String markdown) {
    String content = normalize(markdown);
    if (!content.startsWith(DELIMITER + "\n")) {
      return Optional.empty();
    }
    int start = DELIMITER.length() + 1;
    int line = start;
    while (line <= content.length()) {
      int end = content.indexOf('\n', line);
      int lineEnd = end < 0 ? content.length() : end;
      if (content.substring(line, lineEnd).stripTrailing().equals(DELIMITER)) {
        Map<String, Object> fields = mapping(content.substring(start, line));
        String body = end < 0 ? "" : content.substring(end + 1);
        return Optional.of(new FrontMatter(fields, body.strip()));
      }
      if (end < 0) {
        break;
      }
      line = end + 1;
    }
    throw new IllegalArgumentException("the front matter is not closed with a --- line");
  }

  private static String firstLine(String message) {
    if (message == null) {
      return "unknown error";
    }
    return message.lines().findFirst().orElse(message).strip();
  }
}
