package br.com.refatorado.kaizent.workspace;

import java.util.List;

/** {@code kaizent-workspace.yaml}: the workspace's name, default role and subjects (ADR-0027). */
public record WorkspaceManifest(
    String name, String description, String defaultRole, List<Subject> subjects) {

  public WorkspaceManifest {
    subjects = List.copyOf(subjects);
  }
}
