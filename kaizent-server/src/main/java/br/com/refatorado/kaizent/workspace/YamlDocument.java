package br.com.refatorado.kaizent.workspace;

import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/** Parses YAML files and Markdown front matter into plain maps, with the safe constructor. */
final class YamlDocument {

  private static final String DELIMITER = "---";

  private YamlDocument() {}

  /** Markdown with a required YAML front matter block. */
  record FrontMatter(Map<String, Object> fields, String body) {}

  /**
   * Parses a YAML mapping.
   *
   * @throws IllegalArgumentException if the text is not valid YAML or not a mapping
   */
  static Map<String, Object> mapping(String yaml) {
    Object parsed;
    try {
      parsed = new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
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
   * Splits Markdown into its front matter, delimited by {@code ---} lines, and its body.
   *
   * @throws IllegalArgumentException if the front matter is missing, not closed or not valid
   */
  static FrontMatter frontMatter(String markdown) {
    String content = markdown.replace("\r\n", "\n");
    if (content.startsWith("﻿")) {
      content = content.substring(1);
    }
    if (!content.startsWith(DELIMITER + "\n")) {
      throw new IllegalArgumentException("no front matter: the file must start with a --- line");
    }
    int end = content.indexOf("\n" + DELIMITER, DELIMITER.length());
    if (end < 0) {
      throw new IllegalArgumentException("the front matter is not closed with a --- line");
    }
    Map<String, Object> fields =
        mapping(content.substring(DELIMITER.length() + 1, end + 1)); // keep the last newline
    int bodyStart = content.indexOf('\n', end + 1);
    String body = bodyStart < 0 ? "" : content.substring(bodyStart + 1);
    return new FrontMatter(fields, body.strip());
  }

  private static String firstLine(String message) {
    if (message == null) {
      return "unknown error";
    }
    return message.lines().findFirst().orElse(message).strip();
  }
}
