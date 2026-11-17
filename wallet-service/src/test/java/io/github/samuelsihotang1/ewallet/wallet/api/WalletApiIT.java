package io.github.samuelsihotang1.ewallet.wallet.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samuelsihotang1.ewallet.wallet.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class WalletApiIT {

  @Autowired TestRestTemplate http;
  @Autowired ObjectMapper json;

  @Test
  void opening_a_wallet_returns_201_with_its_location_and_a_zero_idr_balance() throws Exception {
    var response = openWallet(newUser());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    var body = json.readTree(response.getBody());
    assertThat(response.getHeaders().getLocation())
        .hasPath("/api/v1/wallets/" + body.get("walletId").asText());
    assertThat(body.get("balance").asText()).isEqualTo("0.00");
    assertThat(body.get("currency").asText()).isEqualTo("IDR");
  }

  @Test
  void a_second_wallet_for_the_same_user_is_409() throws Exception {
    var user = newUser();
    openWallet(user);

    var response = openWallet(user);

    assertProblem(response, HttpStatus.CONFLICT, "WALLET_ALREADY_EXISTS");
  }

  @Test
  void a_blank_user_id_is_400() throws Exception {
    var response = post("/api/v1/wallets", "{\"userId\":\"\"}", null);

    assertProblem(response, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
  }

  @Test
  void a_top_up_returns_201_and_the_balance_shows_it() throws Exception {
    var walletId = newWalletId();

    var response = topUp(walletId, "150000.50", "key-1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    var receipt = json.readTree(response.getBody());
    assertThat(receipt.get("amount").asText()).isEqualTo("150000.50");
    assertThat(receipt.get("balanceAfter").asText()).isEqualTo("150000.50");
    assertThat(balanceOf(walletId)).isEqualTo("150000.50");
  }

  @Test
  void a_retried_top_up_returns_200_with_the_first_receipt_and_credits_once() throws Exception {
    var walletId = newWalletId();
    var first = json.readTree(topUp(walletId, "50000", "retry-key").getBody());

    var retry = topUp(walletId, "50000", "retry-key");

    assertThat(retry.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(retry.getHeaders().getFirst("Idempotent-Replayed")).isEqualTo("true");
    assertThat(json.readTree(retry.getBody())).isEqualTo(first);
    assertThat(balanceOf(walletId)).isEqualTo("50000.00");
  }

  @Test
  void the_same_key_with_another_amount_is_422() throws Exception {
    var walletId = newWalletId();
    topUp(walletId, "50000", "key-1");

    assertProblem(topUp(walletId, "60000", "key-1"), HttpStatus.UNPROCESSABLE_ENTITY,
        "IDEMPOTENCY_KEY_REUSED");
    assertThat(balanceOf(walletId)).isEqualTo("50000.00");
  }

  @Test
  void a_top_up_without_an_idempotency_key_is_400() throws Exception {
    var walletId = newWalletId();

    var response = post(topUpPath(walletId), "{\"amount\":\"1000\"}", null);

    assertProblem(response, HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED");
  }

  @Test
  void an_amount_with_three_decimals_is_400() throws Exception {
    assertProblem(topUp(newWalletId(), "10.005", "key-1"), HttpStatus.BAD_REQUEST,
        "INVALID_AMOUNT");
  }

  @Test
  void a_top_up_above_the_limit_is_422() throws Exception {
    assertProblem(topUp(newWalletId(), "10000000.01", "key-1"),
        HttpStatus.UNPROCESSABLE_ENTITY, "TOP_UP_LIMIT_EXCEEDED");
  }

  @Test
  void an_unknown_wallet_is_404_and_a_malformed_id_is_400() throws Exception {
    assertProblem(http.getForEntity("/api/v1/wallets/" + UUID.randomUUID(), String.class),
        HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND");
    assertProblem(http.getForEntity("/api/v1/wallets/not-a-uuid", String.class),
        HttpStatus.BAD_REQUEST, "INVALID_REQUEST");
  }

  @Test
  void transactions_list_the_newest_first_with_string_amounts() throws Exception {
    var walletId = newWalletId();
    topUp(walletId, "1000", "key-1");
    topUp(walletId, "2500.75", "key-2");

    var response =
        http.getForEntity(
            "/api/v1/wallets/" + walletId + "/transactions?page=0&size=10", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    var items = json.readTree(response.getBody());
    assertThat(items).hasSize(2);
    assertThat(items.get(0).get("amount").asText()).isEqualTo("2500.75");
    assertThat(items.get(0).get("type").asText()).isEqualTo("TOP_UP");
    assertThat(items.get(0).get("direction").asText()).isEqualTo("CREDIT");
    assertThat(items.get(0).get("balanceAfter").asText()).isEqualTo("3500.75");
    assertThat(items.get(1).get("amount").asText()).isEqualTo("1000.00");
  }

  @Test
  void the_openapi_document_is_served() {
    var response = http.getForEntity("/v3/api-docs", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("/api/v1/wallets/{walletId}/top-ups");
  }


  private static String newUser() {
    return "user-" + UUID.randomUUID();
  }

  private String newWalletId() throws Exception {
    return json.readTree(openWallet(newUser()).getBody()).get("walletId").asText();
  }

  private ResponseEntity<String> openWallet(String userId) {
    return post("/api/v1/wallets", "{\"userId\":\"" + userId + "\"}", null);
  }

  private static String topUpPath(String walletId) {
    return "/api/v1/wallets/" + walletId + "/top-ups";
  }

  private ResponseEntity<String> topUp(String walletId, String amount, String key) {
    return post(topUpPath(walletId), "{\"amount\":\"" + amount + "\"}", key);
  }

  private String balanceOf(String walletId) throws Exception {
    var body = http.getForEntity("/api/v1/wallets/" + walletId, String.class).getBody();
    return json.readTree(body).get("balance").asText();
  }

  private ResponseEntity<String> post(String path, String body, String idempotencyKey) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (idempotencyKey != null) {
      headers.set("Idempotency-Key", idempotencyKey);
    }
    return http.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
  }

  private void assertProblem(ResponseEntity<String> response, HttpStatus status, String code)
      throws Exception {
    assertThat(response.getStatusCode()).isEqualTo(status);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    JsonNode problem = json.readTree(response.getBody());
    assertThat(problem.get("code").asText()).isEqualTo(code);
  }
}
