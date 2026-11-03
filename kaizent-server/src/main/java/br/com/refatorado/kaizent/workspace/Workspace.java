package br.com.refatorado.kaizent.workspace;

import java.nio.file.Path;

/**
 * The resolved workspace folders (ADR-0027). Kaizent never modifies {@code inbox/} and only writes
 * replies to {@code outbox/}.
 *
 * @param root the absolute, normalized workspace folder
 */
public record Workspace(Path root) {

  public Workspace {
    root = root.toAbsolutePath().normalize();
  }

  /** Resolves a configured path against a base folder, usually the working directory. */
  public static Workspace resolve(Path configured, Path base) {
    return new Workspace(configured.isAbsolute() ? configured : base.resolve(configured));
  }

  /** Incoming tickets, one file per ticket (ADR-0028). */
  public Path inbox() {
    return root.resolve("inbox");
  }

  /** Approved replies, written by the delivery step. */
  public Path outbox() {
    return root.resolve("outbox");
  }

  /** {@code kaizent-workspace.yaml}: name, description, subjects and default role. */
  public Path manifest() {
    return root.resolve("kaizent-workspace.yaml");
  }

  /** Role definitions, {@code roles/<role-id>.md}. */
  public Path roles() {
    return root.resolve("roles");
  }

  /** Skills, {@code skills/<skill-name>/SKILL.md}. */
  public Path skills() {
    return root.resolve("skills");
  }

  /** The knowledge base, {@code knowledge/**}{@code /*.md}. */
  public Path knowledge() {
    return root.resolve("knowledge");
  }

  /** Evaluation cases, {@code evals/cases/*.yaml}. */
  public Path evaluationCases() {
    return root.resolve("evals").resolve("cases");
  }

  /** A path relative to the workspace folder, for messages. */
  public Path relativize(Path path) {
    return root.relativize(path.toAbsolutePath().normalize());
  }
}
