package br.com.refatorado.kaizent.workspace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks a loaded workspace (spec 0003 AC-1): every role references declared subjects, existing
 * skills and existing knowledge; the default role exists; every evaluation case references an
 * existing ticket and existing knowledge files that its role may read. Ids must be unique, and
 * skill names follow the Agent Skills format.
 */
public final class WorkspaceValidator {

  /** The subject id that triage uses when no declared subject fits (spec 0006). */
  public static final String UNKNOWN_SUBJECT = "unknown";

  /** The problem types of triage (spec 0006). */
  public static final Set<String> PROBLEM_TYPES = Set.of("BUG", "HOW_TO", "OTHER");

  private static final Pattern ID = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
  // Agent Skills: at most 64 lowercase letters, digits and single hyphens, not at either end.
  private static final int MAX_SKILL_NAME_LENGTH = 64;
  private static final int MAX_SKILL_DESCRIPTION_LENGTH = 1024;
  private static final Set<String> LANGUAGES = Set.of(Locale.getISOLanguages());
  private static final Path KNOWLEDGE = Path.of("knowledge");

  private final WorkspaceContent content;
  private final List<WorkspaceProblem> problems = new ArrayList<>();

  private WorkspaceValidator(WorkspaceContent content) {
    this.content = content;
  }

  /**
   * Returns the problems found while loading the workspace and the broken references between its
   * files, sorted by file. An empty list means the workspace is valid.
   */
  public static List<WorkspaceProblem> validate(WorkspaceContent content) {
    return new WorkspaceValidator(content).validate();
  }

  private List<WorkspaceProblem> validate() {
    problems.addAll(content.problems());
    Set<String> subjects = new HashSet<>();
    content.manifest().ifPresent(manifest -> subjects(manifest, subjects));
    Map<String, RoleDefinition> roles = roles(subjects);
    Set<String> skills = skills();
    content.manifest().ifPresent(manifest -> defaultRole(manifest, roles));
    for (RoleDefinition role : roles.values()) {
      roleReferences(role, subjects, skills);
    }
    evaluationCases(subjects, roles);
    return problems.stream()
        .sorted(Comparator.comparing((WorkspaceProblem p) -> p.file().toString()))
        .toList();
  }

  private void subjects(WorkspaceManifest manifest, Set<String> ids) {
    Path file = manifestFile();
    if (manifest.subjects().isEmpty()) {
      problem(file, "no subjects are declared");
    }
    for (Subject subject : manifest.subjects()) {
      if (!ID.matcher(subject.id()).matches()) {
        problem(file, "subject id '" + subject.id() + "' " + idRule());
      }
      if (UNKNOWN_SUBJECT.equals(subject.id())) {
        problem(file, "'" + UNKNOWN_SUBJECT + "' is reserved and cannot be a subject id");
      }
      if (!ids.add(subject.id())) {
        problem(file, "subject id '" + subject.id() + "' is declared more than once");
      }
    }
  }

  private Map<String, RoleDefinition> roles(Set<String> subjects) {
    Map<String, RoleDefinition> roles = new HashMap<>();
    for (RoleDefinition role : content.roles()) {
      String expected = baseName(role.file(), ".md");
      if (!role.id().equals(expected)) {
        problem(
            role.file(), "id '" + role.id() + "' does not match the file name '" + expected + "'");
      } else if (!ID.matcher(role.id()).matches()) {
        problem(role.file(), "role id '" + role.id() + "' " + idRule());
      }
      if (roles.putIfAbsent(role.id(), role) != null) {
        problem(role.file(), "role id '" + role.id() + "' is used by another role file");
      }
    }
    return roles;
  }

  private Set<String> skills() {
    Set<String> names = new HashSet<>();
    for (SkillDefinition skill : content.skills()) {
      String folder = skill.file().getParent().getFileName().toString();
      if (!skill.name().equals(folder)) {
        problem(
            skill.file(), "name '" + skill.name() + "' does not match the folder '" + folder + "'");
      }
      if (skill.name().length() > MAX_SKILL_NAME_LENGTH || !ID.matcher(skill.name()).matches()) {
        problem(
            skill.file(),
            "name '"
                + skill.name()
                + "' must have at most "
                + MAX_SKILL_NAME_LENGTH
                + " lowercase letters, digits and single hyphens, not at either end");
      }
      if (skill.description() != null
          && skill.description().length() > MAX_SKILL_DESCRIPTION_LENGTH) {
        problem(
            skill.file(),
            "description has more than " + MAX_SKILL_DESCRIPTION_LENGTH + " characters");
      }
      names.add(skill.name());
    }
    return names;
  }

  private void defaultRole(WorkspaceManifest manifest, Map<String, RoleDefinition> roles) {
    if (manifest.defaultRole() != null && !roles.containsKey(manifest.defaultRole())) {
      problem(
          manifestFile(), "defaultRole '" + manifest.defaultRole() + "' is not defined in roles/");
    }
  }

  private void roleReferences(RoleDefinition role, Set<String> subjects, Set<String> skills) {
    // Without a manifest, its absence is the problem to report, not every subject.
    for (String subject : content.manifest().isPresent() ? role.subjects() : List.<String>of()) {
      if (!subjects.contains(subject)) {
        problem(role.file(), "subject '" + subject + "' is not declared in the workspace manifest");
      }
    }
    for (String skill : role.allowedSkills()) {
      if (!skills.contains(skill)) {
        problem(role.file(), "allowed skill '" + skill + "' does not exist in skills/");
      }
    }
    for (String path : role.knowledgePaths()) {
      Path resolved = insideKnowledge(path);
      if (resolved == null) {
        problem(role.file(), "knowledge path '" + path + "' must stay inside knowledge/");
      } else if (!Files.exists(content.workspace().root().resolve(resolved))) {
        problem(role.file(), "knowledge path '" + path + "' does not exist in knowledge/");
      }
    }
  }

  private void evaluationCases(Set<String> subjects, Map<String, RoleDefinition> roles) {
    Map<String, Path> seen = new HashMap<>();
    for (EvaluationCase evaluationCase : content.evaluationCases()) {
      Path file = evaluationCase.file();
      EvaluationCase.Expectation expect = evaluationCase.expect();
      if (!content.ticketIds().contains(evaluationCase.ticket())) {
        problem(file, "ticket '" + evaluationCase.ticket() + "' does not exist in inbox/");
      }
      Path other = seen.putIfAbsent(evaluationCase.ticket(), file);
      if (other != null) {
        problem(file, "ticket '" + evaluationCase.ticket() + "' already has a case in " + other);
      }
      for (String subject :
          content.manifest().isPresent() ? expect.subjects() : List.<String>of()) {
        if (!subjects.contains(subject) && !UNKNOWN_SUBJECT.equals(subject)) {
          problem(file, "expect.subject '" + subject + "' is not a declared subject or 'unknown'");
        }
      }
      if (expect.problemType() != null && !PROBLEM_TYPES.contains(expect.problemType())) {
        problem(
            file, "expect.problemType '" + expect.problemType() + "' must be BUG, HOW_TO or OTHER");
      }
      language(file, "expect.ticketLanguage", expect.ticketLanguage());
      language(file, "expect.replyLanguage", expect.replyLanguage());
      List<RoleDefinition> caseRoles = rolesFor(expect.subjects(), roles);
      for (String cite : expect.citesAny()) {
        citation(file, cite, caseRoles);
      }
    }
  }

  private void citation(Path file, String cite, List<RoleDefinition> caseRoles) {
    Path path = normalizedRelative(cite);
    if (path == null || !path.startsWith(KNOWLEDGE) || path.equals(KNOWLEDGE)) {
      problem(file, "expect.citesAny '" + cite + "' must be a file inside knowledge/");
      return;
    }
    if (!Files.isRegularFile(content.workspace().root().resolve(path))) {
      problem(file, "expect.citesAny '" + cite + "' does not exist");
      return;
    }
    // Invalid knowledge paths are already reported with their role; they grant nothing.
    boolean readable =
        caseRoles.stream()
            .flatMap(role -> role.knowledgePaths().stream())
            .map(WorkspaceValidator::insideKnowledge)
            .anyMatch(allowed -> allowed != null && path.startsWith(allowed));
    if (!caseRoles.isEmpty() && !readable) {
      problem(
          file,
          "expect.citesAny '"
              + cite
              + "' is outside the knowledgePaths of role "
              + String.join(", ", caseRoles.stream().map(RoleDefinition::id).toList()));
    }
  }

  /** The roles that may handle a case: by subject, or the default role for other subjects. */
  private List<RoleDefinition> rolesFor(List<String> subjects, Map<String, RoleDefinition> roles) {
    List<RoleDefinition> result = new ArrayList<>();
    for (String subject : subjects) {
      List<RoleDefinition> matching =
          roles.values().stream().filter(role -> role.subjects().contains(subject)).toList();
      if (matching.isEmpty()) {
        content
            .manifest()
            .map(WorkspaceManifest::defaultRole)
            .map(roles::get)
            .ifPresent(result::add);
      } else {
        result.addAll(matching);
      }
    }
    return result.stream().distinct().toList();
  }

  private void language(Path file, String name, String value) {
    if (value != null && !LANGUAGES.contains(value)) {
      problem(file, name + " '" + value + "' must be an ISO 639-1 code, such as en or pt");
    }
  }

  /** A path relative to {@code knowledge/}, or {@code null} if it leaves that folder. */
  private static Path insideKnowledge(String path) {
    Path resolved = normalizedRelative("knowledge/" + path);
    return resolved == null || !resolved.startsWith(KNOWLEDGE) ? null : resolved;
  }

  /** A normalized relative path, or {@code null} if it is absolute or climbs out with "..". */
  private static Path normalizedRelative(String path) {
    Path parsed;
    try {
      parsed = Path.of(path);
    } catch (RuntimeException e) {
      return null;
    }
    if (parsed.isAbsolute()) {
      return null;
    }
    Path normalized = parsed.normalize();
    return normalized.startsWith("..") ? null : normalized;
  }

  private Path manifestFile() {
    return content.workspace().relativize(content.workspace().manifest());
  }

  private static String baseName(Path file, String extension) {
    String name = file.getFileName().toString();
    return name.endsWith(extension) ? name.substring(0, name.length() - extension.length()) : name;
  }

  private static String idRule() {
    return "must use lowercase letters, digits and single hyphens";
  }

  private void problem(Path file, String message) {
    problems.add(new WorkspaceProblem(file, message));
  }
}
