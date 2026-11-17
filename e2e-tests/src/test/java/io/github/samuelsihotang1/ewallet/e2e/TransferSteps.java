package io.github.samuelsihotang1.ewallet.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Steps for transfer.feature. Each scenario gets a fresh instance, and every user name is
 * suffixed with a run id, so scenarios never share wallets and the stack needs no reset.
 */
public class TransferSteps {

  private static final Duration SAGA_TIMEOUT = Duration.ofSeconds(30);
  /** Matches the wallet service's per-top-up limit. */
  private static final BigDecimal MAX_TOP_UP = new BigDecimal("10000000.00");

  private final String run = UUID.randomUUID().toString().substring(0, 8);
  private final Map<String, String> walletIds = new HashMap<>();
  private Api.Reply lastTransfer;
  private Api.Reply retry;
  private String lastTransferBody;
  private String lastSource;
  private String lastKey;

  private String userId(String name) {
    return name + "-" + run;
  }

  @Given("a wallet {string} holding {string}")
  public void aWalletHolding(String name, String amount) {
    var opened = Api.post(Api.WALLET + "/api/v1/wallets",
        "{\"userId\":\"" + userId(name) + "\"}", Map.of());
    assertThat(opened.status()).isEqualTo(201);
    var walletId = opened.body().get("walletId").asText();
    walletIds.put(name, walletId);

    var left = new BigDecimal(amount);
    var part = 0;
    while (left.signum() > 0) {
      var chunk = left.min(MAX_TOP_UP);
      var topUp = Api.post(Api.WALLET + "/api/v1/wallets/" + walletId + "/top-ups",
          "{\"amount\":\"" + chunk.toPlainString() + "\"}",
          Map.of("Idempotency-Key", "seed-" + part++));
      assertThat(topUp.status()).isEqualTo(201);
      left = left.subtract(chunk);
    }
  }

  @When("{string} transfers {string} to {string} with idempotency key {string}")
  public void transfers(String from, String amount, String to, String key) {
    lastSource = from;
    lastKey = key + "-" + run;
    lastTransferBody = "{\"sourceWalletId\":\"%s\",\"destinationWalletId\":\"%s\",\"amount\":\"%s\"}"
        .formatted(walletIds.get(from), walletIds.get(to), amount);
    lastTransfer = Api.post(Api.TRANSFER + "/api/v1/transfers", lastTransferBody,
        Map.of("Idempotency-Key", lastKey));
    assertThat(lastTransfer.status()).as("transfer request").isEqualTo(202);
  }

  @And("{string} retries the last transfer with the same idempotency key")
  public void retriesTheLastTransfer(String from) {
    assertThat(from).isEqualTo(lastSource);
    retry = Api.post(Api.TRANSFER + "/api/v1/transfers", lastTransferBody,
        Map.of("Idempotency-Key", lastKey));
  }

  @Then("the retry is answered with 200 and the same transfer")
  public void theRetryIsTheSameTransfer() {
    assertThat(retry.status()).isEqualTo(200);
    assertThat(retry.body().get("transferId")).isEqualTo(lastTransfer.body().get("transferId"));
  }

  @Then("the transfer ends as {string}")
  public void theTransferEndsAs(String status) {
    assertThat(finalTransfer().get("status").asText()).isEqualTo(status);
  }

  @Then("the transfer ends as {string} because {string}")
  public void theTransferEndsAsBecause(String status, String reason) {
    var transfer = finalTransfer();
    assertThat(transfer.get("status").asText()).isEqualTo(status);
    assertThat(transfer.get("failureReason").asText()).isEqualTo(reason);
  }

  @And("{string} holds {string}")
  public void holds(String name, String balance) {
    // Balances settle when the saga ends, which the previous step waited for.
    var wallet = Api.get(Api.WALLET + "/api/v1/wallets/" + walletIds.get(name));
    assertThat(wallet.body().get("balance").asText()).isEqualTo(balance);
  }

  @And("{string} is told {string}")
  public void isTold(String name, String text) {
    var expected = text.replace("alice", userId("alice")).replace("bob", userId("bob"));
    await().atMost(SAGA_TIMEOUT).untilAsserted(() -> {
      var notifications =
          Api.get(Api.NOTIFICATION + "/api/v1/users/" + userId(name) + "/notifications");
      assertThat(notifications.body())
          .anySatisfy(n -> assertThat(n.get("message").asText()).contains(expected));
    });
  }

  private com.fasterxml.jackson.databind.JsonNode finalTransfer() {
    var id = lastTransfer.body().get("transferId").asText();
    return await().atMost(SAGA_TIMEOUT).until(
        () -> Api.get(Api.TRANSFER + "/api/v1/transfers/" + id).body(),
        transfer -> switch (transfer.get("status").asText()) {
          case "COMPLETED", "FAILED", "COMPENSATED" -> true;
          default -> false;
        });
  }
}
