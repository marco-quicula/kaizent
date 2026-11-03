package br.com.refatorado.kaizent.workspace.internal;

import br.com.refatorado.kaizent.workspace.Workspace;
import br.com.refatorado.kaizent.workspace.WorkspaceContent;
import br.com.refatorado.kaizent.workspace.WorkspaceLoader;
import br.com.refatorado.kaizent.workspace.WorkspaceProblem;
import br.com.refatorado.kaizent.workspace.WorkspaceValidator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Validates the workspace once the application is ready and logs every problem with its file. The
 * application keeps running: intake and the review console do not need a valid workspace in wave 1,
 * and each problem names the file to fix.
 */
@Component
class WorkspaceStartupCheck {

  private static final Logger log = LoggerFactory.getLogger(WorkspaceStartupCheck.class);

  private final Workspace workspace;

  WorkspaceStartupCheck(Workspace workspace) {
    this.workspace = workspace;
  }

  @EventListener(ApplicationReadyEvent.class)
  void check() {
    WorkspaceContent content = WorkspaceLoader.load(workspace);
    List<WorkspaceProblem> problems = WorkspaceValidator.validate(content);
    if (problems.isEmpty()) {
      log.info(
          "Workspace is valid: {} roles, {} skills, {} tickets, {} evaluation cases",
          content.roles().size(),
          content.skills().size(),
          content.ticketIds().size(),
          content.evaluationCases().size());
      return;
    }
    log.warn("Workspace {} has {} problem(s):", workspace.root(), problems.size());
    problems.forEach(problem -> log.warn("  {}", problem));
  }
}
