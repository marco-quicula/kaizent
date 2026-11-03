package br.com.refatorado.kaizent.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Spec 0003 AC-1 and AC-2: {@link WorkspaceLoader} and {@link WorkspaceValidator}. */
class WorkspaceValidatorTest {

  /** Tests run in {@code kaizent-server/}; the demo workspace is at the repository root. */
  private static final Path PETCLINIC = Path.of("..", "examples", "petclinic");

  @TempDir private Path root;

  @Test
  void thePetclinicWorkspaceIsValid() {
    WorkspaceContent content = WorkspaceLoader.load(new Workspace(PETCLINIC));

    assertThat(WorkspaceValidator.validate(content)).isEmpty();
    WorkspaceManifest manifest = content.manifest().orElseThrow();
    assertThat(manifest.defaultRole()).isEqualTo("support-agent");
    assertThat(manifest.subjects())
        .extracting(Subject::id)
        .containsExactly("owners", "pets", "visits", "technical");
    assertThat(content.roles())
        .extracting(RoleDefinition::id)
        .containsExactly("support-agent", "technical-support-engineer");
    assertThat(content.skills()).hasSize(4);
    assertThat(content.ticketIds()).hasSize(12).contains("PC-001", "PC-012");
    assertThat(content.evaluationCases()).hasSize(12);
  }

  @Test
  void readsTheEvaluationCasesOfThePetclinicWorkspace() {
    WorkspaceContent content = WorkspaceLoader.load(new Workspace(PETCLINIC));

    EvaluationCase pc001 = caseFor(content, "PC-001");
    assertThat(pc001.file()).isEqualTo(Path.of("evals", "cases", "PC-001.yaml"));
    assertThat(pc001.expect().subjects()).containsExactly("owners");
    assertThat(pc001.expect().mentionsAll().getFirst()).contains("10 digits", "ten-digit");
    assertThat(pc001.expect().lowConfidence()).isFalse();
    EvaluationCase pc003 = caseFor(content, "PC-003");
    assertThat(pc003.expect().mentionsAll()).contains(List.of("endereço"));
    assertThat(pc003.expect().ticketLanguage()).isEqualTo("pt");
    EvaluationCase pc012 = caseFor(content, "PC-012");
    assertThat(pc012.expect().subjects()).containsExactly("owners", "unknown");
    assertThat(pc012.expect().citesAny()).isEmpty();
    assertThat(pc012.expect().lowConfidence()).isTrue();
  }

  @Test
  void thePetclinicWorkspaceSaysThatItsDataIsSynthetic() throws IOException {
    assertThat(Files.readString(PETCLINIC.resolve("README.md")))
        .contains("All data in this workspace is synthetic");
  }

  @Nested
  class Fixtures {

    @BeforeEach
    void validWorkspace() throws IOException {
      write(
          "kaizent-workspace.yaml",
          """
          name: Test
          defaultRole: agent
          subjects:
            - id: billing
              name: Billing
              description: Invoices.
            - id: setup
              name: Setup
              description: Installation.
          """);
      write("roles/agent.md", role("agent", "[billing]", "[answer]", "[guides]"));
      write("roles/engineer.md", role("engineer", "[setup]", "[answer]", "[setup.md]"));
      write("skills/answer/SKILL.md", skill("answer"));
      write("knowledge/guides/invoices.md", "# Invoices\n");
      write("knowledge/setup.md", "# Setup\n");
      write("inbox/T-1.md", "---\nid: T-1\ntitle: Invoice\n---\nWhere is my invoice?\n");
      write("inbox/T-2.md", "Installation fails.\n");
      write(
          "evals/cases/T-1.yaml", evaluationCase("T-1", "billing", "knowledge/guides/invoices.md"));
    }

    @Test
    void aConsistentWorkspaceHasNoProblems() {
      assertThat(problems()).isEmpty();
    }

    @Test
    void reportsAMissingManifest() throws IOException {
      Files.delete(root.resolve("kaizent-workspace.yaml"));

      assertThat(problems())
          .containsExactly("kaizent-workspace.yaml: the workspace manifest is missing");
    }

    @Test
    void reportsBrokenReferencesWithTheirFiles() throws IOException {
      write(
          "kaizent-workspace.yaml",
          """
          name: Test
          defaultRole: nobody
          subjects:
            - id: billing
              name: Billing
              description: Invoices.
            - id: billing
              name: Again
              description: Duplicate.
          """);
      write(
          "roles/agent.md",
          role(
              "agent", "[billing, shipping]", "[answer, refund]", "[guides, missing, ../secrets]"));
      write(
          "evals/cases/T-1.yaml",
          """
          ticket: T-9
          expect:
            subject: [billing, shipping]
            problemType: QUESTION
            citesAny: [knowledge/guides/missing.md, knowledge/setup.md, inbox/T-1.md]
            mentionsAll: [invoice]
            mentionsNone: []
            lowConfidence: false
            ticketLanguage: english
            replyLanguage: en
          """);

      assertThat(problems())
          .containsExactlyInAnyOrder(
              "evals/cases/T-1.yaml: ticket 'T-9' does not exist in inbox/",
              "evals/cases/T-1.yaml: expect.subject 'shipping' is not a declared subject or 'unknown'",
              "evals/cases/T-1.yaml: expect.problemType 'QUESTION' must be BUG, HOW_TO or OTHER",
              "evals/cases/T-1.yaml: expect.ticketLanguage 'english' must be an ISO 639-1 code, such as en or pt",
              "evals/cases/T-1.yaml: expect.citesAny 'knowledge/guides/missing.md' does not exist",
              "evals/cases/T-1.yaml: expect.citesAny 'knowledge/setup.md' is outside the knowledgePaths of role agent",
              "evals/cases/T-1.yaml: expect.citesAny 'inbox/T-1.md' must be a file inside knowledge/",
              "kaizent-workspace.yaml: subject id 'billing' is declared more than once",
              "kaizent-workspace.yaml: defaultRole 'nobody' is not defined in roles/",
              "roles/agent.md: subject 'shipping' is not declared in the workspace manifest",
              "roles/agent.md: allowed skill 'refund' does not exist in skills/",
              "roles/agent.md: knowledge path 'missing' does not exist in knowledge/",
              "roles/agent.md: knowledge path '../secrets' must stay inside knowledge/",
              "roles/engineer.md: subject 'setup' is not declared in the workspace manifest");
    }

    @Test
    void reportsFilesThatCannotBeRead() throws IOException {
      write("roles/engineer.md", "You are an engineer.\n");
      write("roles/writer.md", "---\nid: writer\nname: Writer\n");
      write("roles/typo.md", role("tpyo", "[billing]", "[answer]", "[guides]"));
      write("skills/answer/SKILL.md", skill("answer-questions"));
      write("skills/Bad--Name/SKILL.md", skill("Bad--Name"));
      Files.createDirectories(root.resolve("skills/empty"));
      write("evals/cases/T-2.yaml", "ticket: [unclosed\n");

      assertThat(problems())
          .containsExactlyInAnyOrder(
              "evals/cases/T-2.yaml: invalid YAML: while parsing a flow sequence",
              "roles/engineer.md: no front matter: the file must start with a --- line",
              "roles/typo.md: id 'tpyo' does not match the file name 'typo'",
              "roles/writer.md: the front matter is not closed with a --- line",
              "skills/Bad--Name/SKILL.md: name 'Bad--Name' must have at most 64 lowercase"
                  + " letters, digits and single hyphens, not at either end",
              "skills/answer/SKILL.md: name 'answer-questions' does not match the folder 'answer'",
              "skills/empty: the skill folder has no SKILL.md",
              "roles/agent.md: allowed skill 'answer' does not exist in skills/",
              "roles/typo.md: allowed skill 'answer' does not exist in skills/");
    }

    @Test
    void reportsMissingAndMistypedProperties() throws IOException {
      write(
          "roles/agent.md",
          "---\nid: agent\nsubject: [billing]\nallowedSkills: answer\nknowledgePaths: []\n---\n");
      write("skills/answer/SKILL.md", "---\nname: answer\n---\nDo it.\n");
      write(
          "evals/cases/T-1.yaml",
          """
          ticket: T-1
          expect:
            subject: []
            problemType: BUG
            citesAny: []
            mentionsAll: [invoice, [], [paid, 3], {a: b}]
            lowConfidence: maybe
            ticketLanguage: en
          """);

      assertThat(problems())
          .containsExactlyInAnyOrder(
              "evals/cases/T-1.yaml: expect.subject must not be empty",
              "evals/cases/T-1.yaml: expect.mentionsAll[1] must not be an empty list",
              "evals/cases/T-1.yaml: expect.mentionsAll[3] must be a phrase or a list of"
                  + " alternative phrases",
              "evals/cases/T-1.yaml: missing expect.mentionsNone",
              "evals/cases/T-1.yaml: expect.lowConfidence must be true or false",
              "evals/cases/T-1.yaml: missing expect.replyLanguage",
              "roles/agent.md: unknown property subject",
              "roles/agent.md: missing name",
              "roles/agent.md: missing subjects",
              "roles/agent.md: allowedSkills must be a list",
              "roles/agent.md: the persona (the text after the front matter) is empty",
              "skills/answer/SKILL.md: missing description");
    }

    private List<String> problems() {
      return WorkspaceValidator.validate(WorkspaceLoader.load(new Workspace(root))).stream()
          .map(WorkspaceProblem::toString)
          .toList();
    }
  }

  private static EvaluationCase caseFor(WorkspaceContent content, String ticket) {
    return content.evaluationCases().stream()
        .filter(c -> c.ticket().equals(ticket))
        .findFirst()
        .orElseThrow();
  }

  private void write(String path, String content) throws IOException {
    Path file = root.resolve(path);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content);
  }

  private static String role(String id, String subjects, String skills, String knowledge) {
    return """
    ---
    id: %s
    name: %s
    subjects: %s
    allowedSkills: %s
    knowledgePaths: %s
    ---
    You are a helpful %s.
    """
        .formatted(id, id, subjects, skills, knowledge, id);
  }

  private static String skill(String name) {
    return "---\nname: %s\ndescription: Does %s.\n---\nThe instructions.\n".formatted(name, name);
  }

  private static String evaluationCase(String ticket, String subject, String cite) {
    return """
    ticket: %s
    expect:
      subject: %s
      problemType: HOW_TO
      citesAny: [%s]
      mentionsAll:
        - invoice
        - [download, PDF]
      mentionsNone: []
      lowConfidence: false
      ticketLanguage: en
      replyLanguage: en
    """
        .formatted(ticket, subject, cite);
  }
}
