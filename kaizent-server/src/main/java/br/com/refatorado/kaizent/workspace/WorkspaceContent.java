package br.com.refatorado.kaizent.workspace;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Everything read from a workspace by {@link WorkspaceLoader}. Files that could not be read are
 * left out and reported in {@link #problems()}.
 *
 * @param manifest {@code kaizent-workspace.yaml}, if it could be read
 * @param ticketIds the ids of the tickets in {@code inbox/}
 * @param problems the problems found while reading the files
 */
public record WorkspaceContent(
    Workspace workspace,
    Optional<WorkspaceManifest> manifest,
    List<RoleDefinition> roles,
    List<SkillDefinition> skills,
    Set<String> ticketIds,
    List<EvaluationCase> evaluationCases,
    List<WorkspaceProblem> problems) {

  public WorkspaceContent {
    roles = List.copyOf(roles);
    skills = List.copyOf(skills);
    ticketIds = Set.copyOf(ticketIds);
    evaluationCases = List.copyOf(evaluationCases);
    problems = List.copyOf(problems);
  }
}
