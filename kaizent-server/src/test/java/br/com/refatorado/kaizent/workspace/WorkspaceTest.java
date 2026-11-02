package br.com.refatorado.kaizent.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WorkspaceTest {

  @Test
  void resolvesARelativePathAgainstTheBaseFolder() {
    Workspace workspace = Workspace.resolve(Path.of("examples/petclinic"), Path.of("/repo"));

    assertThat(workspace.root()).isEqualTo(Path.of("/repo/examples/petclinic"));
    assertThat(workspace.inbox()).isEqualTo(Path.of("/repo/examples/petclinic/inbox"));
    assertThat(workspace.outbox()).isEqualTo(Path.of("/repo/examples/petclinic/outbox"));
  }

  @Test
  void keepsAnAbsolutePathAndNormalizesIt() {
    Workspace workspace = Workspace.resolve(Path.of("/data/ws/../petclinic"), Path.of("/repo"));

    assertThat(workspace.root()).isEqualTo(Path.of("/data/petclinic"));
  }
}
