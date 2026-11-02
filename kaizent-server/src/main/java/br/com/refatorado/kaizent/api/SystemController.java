package br.com.refatorado.kaizent.api;

import br.com.refatorado.kaizent.api.generated.SystemApi;
import br.com.refatorado.kaizent.api.generated.model.SystemInfo;
import br.com.refatorado.kaizent.workspace.Workspace;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/v1/system}. The profiles are added by spec 0004. */
@RestController
class SystemController implements SystemApi {

  static final String UNKNOWN_VERSION = "unknown";

  private final String version;
  private final Workspace workspace;

  SystemController(ObjectProvider<BuildProperties> buildProperties, Workspace workspace) {
    this.workspace = workspace;
    BuildProperties build = buildProperties.getIfAvailable();
    this.version = build != null ? build.getVersion() : UNKNOWN_VERSION;
  }

  @Override
  public SystemInfo getSystem() {
    return new SystemInfo(version, workspace.root().toString(), List.of());
  }
}
