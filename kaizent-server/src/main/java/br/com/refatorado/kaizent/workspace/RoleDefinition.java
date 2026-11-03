package br.com.refatorado.kaizent.workspace;

import java.nio.file.Path;
import java.util.List;

/**
 * A role from {@code roles/<id>.md}: front matter and persona.
 *
 * @param knowledgePaths files or folders, relative to {@code knowledge/}, that the role may read
 * @param persona the Markdown body
 * @param file the role file, relative to the workspace folder
 */
public record RoleDefinition(
    String id,
    String name,
    List<String> subjects,
    List<String> allowedSkills,
    List<String> knowledgePaths,
    String persona,
    Path file) {

  public RoleDefinition {
    subjects = List.copyOf(subjects);
    allowedSkills = List.copyOf(allowedSkills);
    knowledgePaths = List.copyOf(knowledgePaths);
  }
}
