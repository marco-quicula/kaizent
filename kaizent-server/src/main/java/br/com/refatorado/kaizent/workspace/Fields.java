package br.com.refatorado.kaizent.workspace;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Typed access to the fields of one YAML mapping. Each missing or mistyped field adds a problem for
 * the file and returns a fallback, so that one pass reports every problem of the file.
 */
final class Fields {

  private final Map<String, Object> values;
  private final Path file;
  private final String prefix;
  private final List<WorkspaceProblem> problems;

  Fields(Map<String, Object> values, Path file, List<WorkspaceProblem> problems) {
    this(values, file, "", problems);
  }

  private Fields(
      Map<String, Object> values, Path file, String prefix, List<WorkspaceProblem> problems) {
    this.values = values;
    this.file = file;
    this.prefix = prefix;
    this.problems = problems;
  }

  /** Reports the fields that are not in {@code allowed}, which usually are typos. */
  void allowOnly(Set<String> allowed) {
    Set<String> unknown = new TreeSet<>();
    for (Object key : values.keySet()) {
      if (!allowed.contains(String.valueOf(key))) {
        unknown.add(String.valueOf(key));
      }
    }
    for (String key : unknown) {
      problem("unknown property " + name(key));
    }
  }

  boolean has(String key) {
    return values.containsKey(key);
  }

  /** A required, non-blank text; {@code null} when it is missing. */
  String requiredText(String key) {
    Object value = values.get(key);
    if (value == null || (isScalar(value) && value.toString().isBlank())) {
      problem("missing " + name(key));
      return null;
    }
    if (!isScalar(value)) {
      problem(name(key) + " must be a text");
      return null;
    }
    return value.toString().strip();
  }

  /** An optional text; {@code null} when it is missing. */
  String optionalText(String key) {
    Object value = values.get(key);
    if (value == null) {
      return null;
    }
    if (!isScalar(value)) {
      problem(name(key) + " must be a text");
      return null;
    }
    return value.toString().strip();
  }

  /** A required list of non-blank texts, which may be empty. */
  List<String> requiredTextList(String key) {
    if (!values.containsKey(key)) {
      problem("missing " + name(key));
      return List.of();
    }
    return textList(key, values.get(key));
  }

  /** A required text, or a list of texts. */
  List<String> requiredTextOrList(String key) {
    Object value = values.get(key);
    if (value != null && isScalar(value)) {
      String text = value.toString().strip();
      if (text.isEmpty()) {
        problem("missing " + name(key));
        return List.of();
      }
      return List.of(text);
    }
    List<String> list = requiredTextList(key);
    if (values.containsKey(key) && value instanceof List<?> && list.isEmpty()) {
      problem(name(key) + " must not be empty");
    }
    return list;
  }

  /** A required list whose items are texts or non-empty lists of alternative texts. */
  List<List<String>> requiredGroups(String key) {
    if (!values.containsKey(key)) {
      problem("missing " + name(key));
      return List.of();
    }
    if (!(values.get(key) instanceof List<?> items)) {
      problem(name(key) + " must be a list");
      return List.of();
    }
    List<List<String>> groups = new ArrayList<>();
    for (int i = 0; i < items.size(); i++) {
      Object item = items.get(i);
      String itemName = name(key) + "[" + i + "]";
      if (item instanceof List<?> alternatives) {
        List<String> texts = textList(key + "[" + i + "]", alternatives);
        if (alternatives.isEmpty()) {
          problem(itemName + " must not be an empty list");
        } else if (texts.size() == alternatives.size()) {
          groups.add(texts);
        }
      } else if (item != null && isScalar(item) && !item.toString().isBlank()) {
        groups.add(List.of(item.toString().strip()));
      } else {
        problem(itemName + " must be a phrase or a list of alternative phrases");
      }
    }
    return groups;
  }

  /** A required boolean; {@code false} when it is missing. */
  boolean requiredBoolean(String key) {
    Object value = values.get(key);
    if (value instanceof Boolean bool) {
      return bool;
    }
    problem(value == null ? "missing " + name(key) : name(key) + " must be true or false");
    return false;
  }

  /** A required nested mapping; an empty one when it is missing. */
  Fields requiredMapping(String key) {
    Object value = values.get(key);
    if (value instanceof Map<?, ?> map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> nested = (Map<String, Object>) map;
      return new Fields(nested, file, name(key) + ".", problems);
    }
    problem(value == null ? "missing " + name(key) : name(key) + " must be a mapping");
    return new Fields(Map.of(), file, name(key) + ".", new ArrayList<>());
  }

  /** The items of a required list of mappings. */
  List<Fields> requiredMappingList(String key) {
    if (!(values.get(key) instanceof List<?> items)) {
      problem(values.containsKey(key) ? name(key) + " must be a list" : "missing " + name(key));
      return List.of();
    }
    List<Fields> result = new ArrayList<>();
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i) instanceof Map<?, ?> map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) map;
        result.add(new Fields(nested, file, name(key) + "[" + i + "].", problems));
      } else {
        problem(name(key) + "[" + i + "] must be a mapping");
      }
    }
    return result;
  }

  void problem(String message) {
    problems.add(new WorkspaceProblem(file, message));
  }

  String name(String key) {
    return prefix + key;
  }

  private List<String> textList(String key, Object value) {
    if (!(value instanceof List<?> items)) {
      problem(name(key) + " must be a list");
      return List.of();
    }
    List<String> texts = new ArrayList<>();
    for (int i = 0; i < items.size(); i++) {
      Object item = items.get(i);
      if (item == null || !isScalar(item) || item.toString().isBlank()) {
        problem(name(key) + "[" + i + "] must be a non-empty text");
      } else {
        texts.add(item.toString().strip());
      }
    }
    return texts;
  }

  private static boolean isScalar(Object value) {
    return !(value instanceof Map<?, ?>) && !(value instanceof List<?>);
  }
}
