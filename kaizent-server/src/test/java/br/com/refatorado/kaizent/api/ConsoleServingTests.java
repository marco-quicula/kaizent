package br.com.refatorado.kaizent.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.refatorado.kaizent.TestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/** The console is served at / with a fallback for client-side routes (spec 0001 AC-4, T8). */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ConsoleServingTests {

  private static final Pattern SCRIPT = Pattern.compile("src=\"(/assets/[^\"]+\\.js)\"");

  @LocalServerPort private int port;

  @Test
  void servesTheConsoleAtTheRoot() throws Exception {
    HttpResponse<String> response = get("/");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("<title>Kaizent</title>", "<div id=\"root\">");
  }

  @Test
  void servesTheConsoleForClientSideRoutes() throws Exception {
    for (String route : new String[] {"/runs", "/system", "/runs/123"}) {
      HttpResponse<String> response = get(route);

      assertThat(response.statusCode()).as(route).isEqualTo(200);
      assertThat(response.body()).as(route).contains("<div id=\"root\">");
    }
  }

  @Test
  void servesTheBundledAssets() throws Exception {
    Matcher script = SCRIPT.matcher(get("/").body());
    assertThat(script.find()).isTrue();

    HttpResponse<String> response = get(script.group(1));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(type -> assertThat(type).contains("javascript"));
  }

  @Test
  void doesNotFallBackForServerPaths() throws Exception {
    assertThat(get("/api/v1/does-not-exist").statusCode()).isEqualTo(404);
    assertThat(get("/actuator/does-not-exist").statusCode()).isEqualTo(404);
  }

  private HttpResponse<String> get(String path) throws Exception {
    try (HttpClient client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build()) {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build();
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
  }
}
