package br.com.refatorado.kaizent.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The paths come from a temporary folder, which is absolute on every operating system: "/repo" is
 * not absolute on Windows, where it lacks a drive letter.
 */
class WorkspaceTest {

  @TempDir Path base;

  @Test
  void resolvesARelativePathAgainstTheBaseFolder() {
    Workspace workspace = Workspace.resolve(Path.of("examples", "petclinic"), base);

    Path root = base.resolve("examples").resolve("petclinic");
    assertThat(workspace.root()).isEqualTo(root);
    assertThat(workspace.inbox()).isEqualTo(root.resolve("inbox"));
    assertThat(workspace.outbox()).isEqualTo(root.resolve("outbox"));
  }

  @Test
  void keepsAnAbsolutePathAndNormalizesIt() {
    Path configured = base.resolve("data").resolve("ws").resolve("..").resolve("petclinic");

    Workspace workspace = Workspace.resolve(configured, Path.of("unused"));

    assertThat(configured.isAbsolute()).isTrue();
    assertThat(workspace.root()).isEqualTo(base.resolve("data").resolve("petclinic"));
  }
}
