package br.com.refatorado.kaizent;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Starts the full application against PostgreSQL 18 (spec 0001 AC-3, AC-5). */
@KaizentIntegrationTest
class KaizentApplicationTests {

  @LocalServerPort private int port;

  @Value("${server.address}")
  private String serverAddress;

  @Autowired private Flyway flyway;

  @Test
  void healthIsUpIncludingTheDatabase() throws Exception {
    HttpResponse<String> response = get("/actuator/health");

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode health = JsonMapper.shared().readTree(response.body());
    assertThat(health.at("/status").asString()).isEqualTo("UP");
    assertThat(health.at("/components/db/status").asString()).isEqualTo("UP");
  }

  @Test
  void migrationsAreApplied() {
    MigrationInfo[] applied = flyway.info().applied();

    assertThat(applied)
        .extracting(info -> info.getVersion().getVersion())
        .containsExactly("1", "2", "3");
    assertThat(applied).allMatch(info -> info.getState().isApplied());
  }

  @Test
  void bindsToLoopbackByDefault() {
    assertThat(serverAddress).isEqualTo("127.0.0.1");
  }

  private HttpResponse<String> get(String path) throws Exception {
    try (HttpClient client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build()) {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build();
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
  }
}
