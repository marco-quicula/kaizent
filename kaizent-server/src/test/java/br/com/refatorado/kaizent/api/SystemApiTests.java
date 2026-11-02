package br.com.refatorado.kaizent.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.KaizentIntegrationTest;
import br.com.refatorado.kaizent.workspace.Workspace;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** {@code GET /api/v1/system} and the published contract (spec 0001 AC-8). */
@KaizentIntegrationTest
class SystemApiTests {

  @LocalServerPort private int port;

  @Autowired private BuildProperties buildProperties;
  @Autowired private Workspace workspace;

  @Test
  void returnsTheApplicationVersion() throws Exception {
    HttpResponse<String> response = get("/api/v1/system");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type")).hasValue("application/json");
    JsonNode system = JsonMapper.shared().readTree(response.body());
    assertThat(system.at("/version").asString())
        .isEqualTo(buildProperties.getVersion())
        .isNotBlank();
    assertThat(system.at("/workspace").asString()).isEqualTo(workspace.root().toString());
    assertThat(system.at("/profiles").isArray()).isTrue();
    assertThat(system.at("/profiles").isEmpty()).isTrue();
  }

  @Test
  void servesTheContract() throws Exception {
    HttpResponse<String> response = get("/api/v1/openapi.yaml");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).startsWith("openapi: 3.1.0").contains("operationId: getSystem");
  }

  private HttpResponse<String> get(String path) throws Exception {
    try (HttpClient client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build()) {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build();
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
  }
}
