package br.com.refatorado.kaizent.api;

import br.com.refatorado.kaizent.api.generated.SystemApi;
import br.com.refatorado.kaizent.api.generated.model.CostType;
import br.com.refatorado.kaizent.api.generated.model.StepProfile;
import br.com.refatorado.kaizent.api.generated.model.SystemInfo;
import br.com.refatorado.kaizent.llm.LlmProfiles;
import br.com.refatorado.kaizent.workspace.Workspace;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/v1/system}: the version, the workspace and the profile of each pipeline step
 * (spec 0004 AC-12).
 */
@RestController
class SystemController implements SystemApi {

  static final String UNKNOWN_VERSION = "unknown";

  private final String version;
  private final Workspace workspace;
  private final LlmProfiles profiles;

  SystemController(
      ObjectProvider<BuildProperties> buildProperties, Workspace workspace, LlmProfiles profiles) {
    this.workspace = workspace;
    this.profiles = profiles;
    BuildProperties build = buildProperties.getIfAvailable();
    this.version = build != null ? build.getVersion() : UNKNOWN_VERSION;
  }

  @Override
  public SystemInfo getSystem() {
    List<StepProfile> steps =
        profiles.byStep().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry ->
                new StepProfile(
                        entry.getKey(),
                        entry.getValue().adapter(),
                        CostType.fromValue(profiles.costType(entry.getValue()).name()))
                    .model(entry.getValue().model())
                    .preset(entry.getValue().preset()))
            .toList();
    return new SystemInfo(version, workspace.root().toString(), steps);
  }
}
