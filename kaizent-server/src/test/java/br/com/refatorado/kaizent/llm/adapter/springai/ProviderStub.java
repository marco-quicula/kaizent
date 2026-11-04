package br.com.refatorado.kaizent.llm.adapter.springai;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A provider API stub on 127.0.0.1 for adapter tests: it answers the scripted responses in order
 * and records the requests. CI never calls real models (ADR-0020).
 */
final class ProviderStub implements AutoCloseable {

  record Answer(int status, Map<String, String> headers, String body) {}

  record Received(String path, Map<String, List<String>> headers, String body) {}

  private final HttpServer server;
  private final Deque<Answer> answers = new ArrayDeque<>();
  private final List<Received> received = new CopyOnWriteArrayList<>();

  ProviderStub() throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/",
        exchange -> {
          received.add(
              new Received(
                  exchange.getRequestURI().getPath(),
                  Map.copyOf(exchange.getRequestHeaders()),
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
          Answer answer;
          synchronized (answers) {
            answer = answers.isEmpty() ? new Answer(500, Map.of(), "{}") : answers.removeFirst();
          }
          byte[] body = answer.body().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          answer.headers().forEach(exchange.getResponseHeaders()::add);
          exchange.sendResponseHeaders(answer.status(), body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
  }

  ProviderStub answer(int status, String body) {
    return answer(status, Map.of(), body);
  }

  ProviderStub answer(int status, Map<String, String> headers, String body) {
    synchronized (answers) {
      answers.add(new Answer(status, headers, body));
    }
    return this;
  }

  String url() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  List<Received> received() {
    return received;
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
