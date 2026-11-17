package io.github.samuelsihotang1.ewallet.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** A thin JSON-over-HTTP client for the three services, as a mobile app would call them. */
final class Api {

  static final String WALLET = System.getProperty("e2e.wallet-url", "http://localhost:8081");
  static final String TRANSFER = System.getProperty("e2e.transfer-url", "http://localhost:8082");
  static final String NOTIFICATION =
      System.getProperty("e2e.notification-url", "http://localhost:8083");

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  private Api() {}

  /** A response's status code and its parsed JSON body. */
  record Reply(int status, JsonNode body) {}

  static Reply get(String url) {
    return send(HttpRequest.newBuilder(URI.create(url)).GET(), Map.of());
  }

  static Reply post(String url, String json, Map<String, String> headers) {
    return send(
        HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json)),
        headers);
  }

  private static Reply send(HttpRequest.Builder request, Map<String, String> headers) {
    headers.forEach(request::header);
    try {
      var response = HTTP.send(
          request.timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofString());
      var body = response.body().isEmpty() ? null : JSON.readTree(response.body());
      return new Reply(response.statusCode(), body);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
