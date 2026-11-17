package io.github.samuelsihotang1.ewallet.transfer.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samuelsihotang1.ewallet.transfer.TestcontainersConfiguration;
import io.github.samuelsihotang1.ewallet.transfer.application.WalletDirectory;
import io.github.samuelsihotang1.ewallet.transfer.application.WalletDirectoryUnavailableException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TransferApiIT {

  @Autowired TestRestTemplate http;
  @Autowired ObjectMapper json;
  @Autowired JdbcClient jdbc;
  @MockitoBean WalletDirectory wallets;

  private final UUID alice = UUID.randomUUID();
  private final UUID bob = UUID.randomUUID();

  @BeforeEach
  void knownWallets() {
    given(wallets.ownerOf(any())).willReturn(Optional.empty());
    given(wallets.ownerOf(alice)).willReturn(Optional.of("user-alice"));
    given(wallets.ownerOf(bob)).willReturn(Optional.of("user-bob"));
  }

  @Test
  void a_transfer_is_accepted_with_202_and_its_location() throws Exception {
    var response = transfer(alice, bob, "150000.50", "key-1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    var body = json.readTree(response.getBody());
    var id = body.get("transferId").asText();
    assertThat(response.getHeaders().getLocation()).hasPath("/api/v1/transfers/" + id);
    assertThat(body.get("status").asText()).isEqualTo("PENDING");
    assertThat(body.get("amount").asText()).isEqualTo("150000.50");
    assertThat(body.get("sourceWalletId").asText()).isEqualTo(alice.toString());
    assertThat(body.get("destinationWalletId").asText()).isEqualTo(bob.toString());
    assertThat(outboxTypes(id)).containsExactly("TransferRequested");
  }

  @Test
  void a_retry_returns_200_with_the_same_transfer_and_requests_it_once() throws Exception {
    var first = json.readTree(transfer(alice, bob, "10.00", "retry-key").getBody());

    var retry = transfer(alice, bob, "10.00", "retry-key");

    assertThat(retry.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(retry.getHeaders().getFirst("Idempotent-Replayed")).isEqualTo("true");
    assertThat(json.readTree(retry.getBody())).isEqualTo(first);
    assertThat(outboxTypes(first.get("transferId").asText())).hasSize(1);
  }

  @Test
  void the_same_key_with_another_amount_is_422() throws Exception {
    transfer(alice, bob, "10.00", "reused-key");

    assertProblem(transfer(alice, bob, "11.00", "reused-key"),
        HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REUSED");
  }

  @Test
  void a_missing_idempotency_key_is_400() throws Exception {
    assertProblem(transfer(alice, bob, "10.00", null),
        HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED");
  }

  @Test
  void an_unknown_destination_is_404() throws Exception {
    assertProblem(transfer(alice, UUID.randomUUID(), "10.00", "key-1"),
        HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND");
  }

  @Test
  void a_transfer_to_the_same_wallet_is_422() throws Exception {
    assertProblem(transfer(alice, alice, "10.00", "key-1"),
        HttpStatus.UNPROCESSABLE_ENTITY, "SAME_WALLET");
  }

  @Test
  void an_amount_with_three_decimals_is_400() throws Exception {
    assertProblem(transfer(alice, bob, "10.005", "key-1"),
        HttpStatus.BAD_REQUEST, "INVALID_AMOUNT");
  }

  @Test
  void a_transfer_above_the_limit_is_422() throws Exception {
    assertProblem(transfer(alice, bob, "10000000.01", "key-1"),
        HttpStatus.UNPROCESSABLE_ENTITY, "TRANSFER_LIMIT_EXCEEDED");
  }

  @Test
  void when_the_wallet_service_is_down_the_answer_is_503() throws Exception {
    given(wallets.ownerOf(alice))
        .willThrow(new WalletDirectoryUnavailableException("down", null));

    assertProblem(transfer(alice, bob, "10.00", "key-1"),
        HttpStatus.SERVICE_UNAVAILABLE, "WALLET_SERVICE_UNAVAILABLE");
  }

  @Test
  void a_transfer_can_be_read_back() throws Exception {
    var id = json.readTree(transfer(alice, bob, "10.00", "key-1").getBody())
        .get("transferId").asText();

    var response = http.getForEntity("/api/v1/transfers/" + id, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(json.readTree(response.getBody()).get("status").asText()).isEqualTo("PENDING");
  }

  @Test
  void an_unknown_transfer_is_404() throws Exception {
    assertProblem(http.getForEntity("/api/v1/transfers/" + UUID.randomUUID(), String.class),
        HttpStatus.NOT_FOUND, "TRANSFER_NOT_FOUND");
  }


  private ResponseEntity<String> transfer(UUID from, UUID to, String amount, String key) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (key != null) {
      headers.set("Idempotency-Key", key);
    }
    var body = """
        {"sourceWalletId":"%s","destinationWalletId":"%s","amount":"%s"}"""
        .formatted(from, to, amount);
    return http.postForEntity("/api/v1/transfers", new HttpEntity<>(body, headers), String.class);
  }

  private java.util.List<String> outboxTypes(String transferId) {
    return jdbc.sql("SELECT event_type FROM outbox_event WHERE partition_key = :key ORDER BY seq")
        .param("key", transferId)
        .query(String.class)
        .list();
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
