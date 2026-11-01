package br.com.refatorado.kaizent.api;

import br.com.refatorado.kaizent.api.generated.SystemApi;
import br.com.refatorado.kaizent.api.generated.model.SystemInfo;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/v1/system}. The workspace and profiles are added by specs 0002 and 0004. */
@RestController
class SystemController implements SystemApi {

  static final String UNKNOWN_VERSION = "unknown";

  private final String version;

  SystemController(ObjectProvider<BuildProperties> buildProperties) {
    BuildProperties build = buildProperties.getIfAvailable();
    this.version = build != null ? build.getVersion() : UNKNOWN_VERSION;
  }

  @Override
  public SystemInfo getSystem() {
    return new SystemInfo(version, null, List.of());
  }
}
