package br.com.refatorado.kaizent.llm.adapter.springai;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps the Gemini API's error status (for example {@code RESOURCE_EXHAUSTED}). The google-genai
 * SDK builds its {@code ApiException} from the HTTP reason phrase ("Too Many Requests", or nothing
 * over HTTP/2) and drops the {@code error.status} of the JSON body. This interceptor puts that
 * status in the reason phrase of an error response, so {@code ApiException.status()} returns it
 * (spec 0004 AC-4).
 */
final class GeminiErrorStatus implements Interceptor {

  private static final long MAX_BODY = 1024 * 1024;

  @Override
  public Response intercept(Chain chain) throws IOException {
    Response response = chain.proceed(chain.request());
    if (response.isSuccessful()) {
      return response;
    }
    String status = status(response.peekBody(MAX_BODY).string());
    return status == null ? response : response.newBuilder().message(status).build();
  }

  static String status(String body) {
    try {
      JsonNode status = JsonMapper.shared().readTree(body).path("error").path("status");
      return status.isString() && !status.asString().isBlank() ? status.asString() : null;
    } catch (RuntimeException e) {
      return null; // Not JSON: keep the reason phrase.
    }
  }
}
