package br.com.refatorado.kaizent.workspace;

import br.com.refatorado.kaizent.workspace.EvaluationCase.Expectation;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Reads a workspace (ADR-0027): the manifest, the roles, the skills' front matter, the ticket ids
 * and the evaluation cases. It never fails on bad content: every file that cannot be read is
 * reported as a {@link WorkspaceProblem}, and {@link WorkspaceValidator} checks the references
 * between files.
 */
public final class WorkspaceLoader {

  static final String SKILL_FILE = "SKILL.md";

  private static final Set<String> MANIFEST_FIELDS =
      Set.of("name", "description", "defaultRole", "subjects");
  private static final Set<String> SUBJECT_FIELDS = Set.of("id", "name", "description");
  private static final Set<String> ROLE_FIELDS =
      Set.of("id", "name", "subjects", "allowedSkills", "knowledgePaths");
  // The optional fields of the Agent Skills specification are allowed too.
  private static final Set<String> SKILL_FIELDS =
      Set.of("name", "description", "license", "compatibility", "metadata", "allowed-tools");
  private static final Set<String> CASE_FIELDS = Set.of("ticket", "expect");
  private static final Set<String> EXPECT_FIELDS =
      Set.of(
          "subject",
          "problemType",
          "citesAny",
          "mentionsAll",
          "mentionsNone",
          "lowConfidence",
          "ticketLanguage",
          "replyLanguage");

  private final Workspace workspace;
  private final List<WorkspaceProblem> problems = new ArrayList<>();

  private WorkspaceLoader(Workspace workspace) {
    this.workspace = workspace;
  }

  /** Reads the workspace. Missing folders are empty, except the manifest, which is required. */
  public static WorkspaceContent load(Workspace workspace) {
    return new WorkspaceLoader(workspace).load();
  }

  private WorkspaceContent load() {
    Optional<WorkspaceManifest> manifest = manifest();
    List<RoleDefinition> roles = new ArrayList<>();
    for (Path file : files(workspace.roles(), WorkspaceLoader::isMarkdown)) {
      role(file).ifPresent(roles::add);
    }
    List<SkillDefinition> skills = new ArrayList<>();
    for (Path folder : files(workspace.skills(), Files::isDirectory)) {
      skill(folder).ifPresent(skills::add);
    }
    Set<String> ticketIds = new TreeSet<>();
    for (Path file : files(workspace.inbox(), WorkspaceLoader::isMarkdown)) {
      ticketId(file).ifPresent(ticketIds::add);
    }
    List<EvaluationCase> cases = new ArrayList<>();
    for (Path file : files(workspace.evaluationCases(), WorkspaceLoader::isYaml)) {
      evaluationCase(file).ifPresent(cases::add);
    }
    return new WorkspaceContent(workspace, manifest, roles, skills, ticketIds, cases, problems);
  }

  private Optional<WorkspaceManifest> manifest() {
    Path file = workspace.manifest();
    if (!Files.isRegularFile(file)) {
      problem(file, "the workspace manifest is missing");
      return Optional.empty();
    }
    return read(file)
        .flatMap(text -> mapping(file, text))
        .map(values -> {
          Fields fields = fields(values, file);
          fields.allowOnly(MANIFEST_FIELDS);
          String name = fields.requiredText("name");
          String description = fields.optionalText("description");
          String defaultRole = fields.requiredText("defaultRole");
          List<Subject> subjects = new ArrayList<>();
          for (Fields subject : fields.requiredMappingList("subjects")) {
            subject.allowOnly(SUBJECT_FIELDS);
            String id = subject.requiredText("id");
            String subjectName = subject.requiredText("name");
            String subjectDescription = subject.requiredText("description");
            if (id != null) {
              subjects.add(new Subject(id, subjectName, subjectDescription));
            }
          }
          return new WorkspaceManifest(name, description, defaultRole, subjects);
        });
  }

  private Optional<RoleDefinition> role(Path file) {
    return frontMatter(file)
        .map(document -> {
          Fields fields = fields(document.fields(), file);
          fields.allowOnly(ROLE_FIELDS);
          String id = fields.requiredText("id");
          String name = fields.requiredText("name");
          List<String> subjects = fields.requiredTextList("subjects");
          List<String> skills = fields.requiredTextList("allowedSkills");
          List<String> knowledge = fields.requiredTextList("knowledgePaths");
          if (document.body().isEmpty()) {
            fields.problem("the persona (the text after the front matter) is empty");
          }
          return id == null
              ? null
              : new RoleDefinition(
                  id, name, subjects, skills, knowledge, document.body(), relative(file));
        });
  }

  private Optional<SkillDefinition> skill(Path folder) {
    Path file = folder.resolve(SKILL_FILE);
    if (!Files.isRegularFile(file)) {
      problem(folder, "the skill folder has no " + SKILL_FILE);
      return Optional.empty();
    }
    return frontMatter(file)
        .map(document -> {
          Fields fields = fields(document.fields(), file);
          fields.allowOnly(SKILL_FIELDS);
          String name = fields.requiredText("name");
          String description = fields.requiredText("description");
          if (document.body().isEmpty()) {
            fields.problem("the skill has no instructions after the front matter");
          }
          return name == null ? null : new SkillDefinition(name, description, relative(file));
        });
  }

  /**
   * The ticket id, as intake reads it (ADR-0028), with the same parser: the {@code id} of the front
   * matter, or else the file name. A ticket that intake would skip has no id. Ticket validation
   * itself belongs to intake (spec 0005).
   */
  private Optional<String> ticketId(Path file) {
    String fileName = file.getFileName().toString();
    String fallback = fileName.substring(0, fileName.length() - ".md".length());
    try {
      // Decoded as intake decodes it: malformed bytes are replaced, not rejected.
      String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
      Object id =
          YamlDocument.frontMatter(text).map(document -> document.fields().get("id")).orElse(null);
      return Optional.of(id == null || id.toString().isBlank() ? fallback : id.toString().strip());
    } catch (IOException e) {
      problem(file, "cannot read the file: " + e.getMessage());
    } catch (IllegalArgumentException e) {
      problem(file, "the ticket cannot be read, so intake skips it: " + e.getMessage());
    }
    return Optional.empty();
  }

  private Optional<EvaluationCase> evaluationCase(Path file) {
    return read(file)
        .flatMap(text -> mapping(file, text))
        .map(values -> {
          Fields fields = fields(values, file);
          fields.allowOnly(CASE_FIELDS);
          String ticket = fields.requiredText("ticket");
          Fields expect = fields.requiredMapping("expect");
          expect.allowOnly(EXPECT_FIELDS);
          Expectation expectation =
              new Expectation(
                  expect.requiredTextOrList("subject"),
                  expect.requiredText("problemType"),
                  expect.requiredTextList("citesAny"),
                  expect.requiredGroups("mentionsAll"),
                  expect.requiredTextList("mentionsNone"),
                  expect.requiredBoolean("lowConfidence"),
                  expect.requiredText("ticketLanguage"),
                  expect.requiredText("replyLanguage"));
          return ticket == null ? null : new EvaluationCase(ticket, expectation, relative(file));
        });
  }

  private Optional<YamlDocument.FrontMatter> frontMatter(Path file) {
    return read(file)
        .flatMap(text -> {
          try {
            Optional<YamlDocument.FrontMatter> document = YamlDocument.frontMatter(text);
            if (document.isEmpty()) {
              problem(file, "no front matter: the file must start with a --- line");
            }
            return document;
          } catch (IllegalArgumentException e) {
            problem(file, e.getMessage());
            return Optional.empty();
          }
        });
  }

  private Optional<Map<String, Object>> mapping(Path file, String text) {
    try {
      return Optional.of(YamlDocument.mapping(text));
    } catch (IllegalArgumentException e) {
      problem(file, e.getMessage());
      return Optional.empty();
    }
  }

  private Optional<String> read(Path file) {
    try {
      return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
    } catch (CharacterCodingException e) {
      problem(file, "the file is not valid UTF-8");
    } catch (IOException e) {
      problem(file, "cannot read the file: " + e.getMessage());
    }
    return Optional.empty();
  }

  private List<Path> files(Path folder, Predicate<Path> filter) {
    if (!Files.isDirectory(folder)) {
      return List.of();
    }
    try (Stream<Path> entries = Files.list(folder)) {
      return entries.filter(filter).sorted().toList();
    } catch (IOException e) {
      problem(folder, "cannot list the folder: " + e.getMessage());
      return List.of();
    }
  }

  private Fields fields(Map<String, Object> values, Path file) {
    return new Fields(values, relative(file), problems);
  }

  private void problem(Path file, String message) {
    problems.add(new WorkspaceProblem(relative(file), message));
  }

  private Path relative(Path file) {
    return workspace.relativize(file);
  }

  private static boolean isMarkdown(Path file) {
    return Files.isRegularFile(file) && file.getFileName().toString().endsWith(".md");
  }

  private static boolean isYaml(Path file) {
    String name = file.getFileName().toString();
    return Files.isRegularFile(file) && (name.endsWith(".yaml") || name.endsWith(".yml"));
  }
}
