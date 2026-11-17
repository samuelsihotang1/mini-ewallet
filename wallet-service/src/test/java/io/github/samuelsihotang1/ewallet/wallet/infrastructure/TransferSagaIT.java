package io.github.samuelsihotang1.ewallet.wallet.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.samuelsihotang1.ewallet.events.EventCodec;
import io.github.samuelsihotang1.ewallet.events.TransferCompensationRequested;
import io.github.samuelsihotang1.ewallet.events.TransferDebited;
import io.github.samuelsihotang1.ewallet.events.TransferEvent;
import io.github.samuelsihotang1.ewallet.events.TransferRequested;
import io.github.samuelsihotang1.ewallet.events.Topics;
import io.github.samuelsihotang1.ewallet.wallet.TestcontainersConfiguration;
import io.github.samuelsihotang1.ewallet.wallet.application.OpenWalletService;
import io.github.samuelsihotang1.ewallet.wallet.application.TopUpCommand;
import io.github.samuelsihotang1.ewallet.wallet.application.TopUpService;
import io.github.samuelsihotang1.ewallet.wallet.application.WalletQueryService;
import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.SystemAccount;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;

/** The wallet's side of the transfer saga, driven through real Kafka and MySQL. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TransferSagaIT {

  private static final Duration WAIT = Duration.ofSeconds(30);

  @Autowired OpenWalletService openWallet;
  @Autowired TopUpService topUps;
  @Autowired WalletQueryService queries;
  @Autowired KafkaTemplate<String, String> kafka;
  @Autowired EventCodec codec;
  @Autowired JdbcClient jdbc;

  private WalletId walletWith(String balance) {
    var wallet = openWallet.open("user-" + UUID.randomUUID());
    if (Money.of(balance).isPositive()) {
      topUps.topUp(new TopUpCommand(wallet.id(), Money.of(balance), "seed"));
    }
    return wallet.id();
  }

  private void deliver(TransferEvent event) {
    kafka.send(Topics.TRANSFER_EVENTS, event.partitionKey(), codec.encode(event)).join();
  }

  private Money balanceOf(WalletId id) {
    return queries.get(id).balance();
  }

  /** Types of the wallet events written to the outbox for one transfer, oldest first. */
  private List<String> announced(UUID transferId) {
    return jdbc.sql(
            "SELECT event_type FROM outbox_event WHERE partition_key = :key ORDER BY seq")
        .param("key", transferId.toString())
        .query(String.class)
        .list();
  }

  @Test
  void a_requested_transfer_debits_the_source_once_even_when_delivered_twice() {
    var source = walletWith("100.00");
    var destination = walletWith("0");
    var transferId = UUID.randomUUID();
    var requested =
        new TransferRequested(
            UUID.randomUUID(), transferId, source.value(), destination.value(), "40.00",
            Instant.now());

    deliver(requested);
    deliver(requested);

    await().atMost(WAIT).untilAsserted(() ->
        assertThat(announced(transferId)).containsExactly("WalletDebited"));
    assertThat(balanceOf(source)).isEqualTo(Money.of("60.00"));
  }

  @Test
  void a_debited_transfer_credits_the_destination() {
    var source = walletWith("100.00");
    var destination = walletWith("0");
    var transferId = UUID.randomUUID();
    deliver(new TransferRequested(
        UUID.randomUUID(), transferId, source.value(), destination.value(), "40.00",
        Instant.now()));
    await().atMost(WAIT).until(() -> announced(transferId).contains("WalletDebited"));

    deliver(new TransferDebited(
        UUID.randomUUID(), transferId, destination.value(), "40.00", Instant.now()));

    await().atMost(WAIT).untilAsserted(() ->
        assertThat(announced(transferId)).containsExactly("WalletDebited", "WalletCredited"));
    assertThat(balanceOf(destination)).isEqualTo(Money.of("40.00"));
  }

  @Test
  void insufficient_balance_is_announced_as_a_failed_debit() {
    var source = walletWith("10.00");
    var transferId = UUID.randomUUID();

    deliver(new TransferRequested(
        UUID.randomUUID(), transferId, source.value(), WalletId.random().value(), "40.00",
        Instant.now()));

    await().atMost(WAIT).untilAsserted(() ->
        assertThat(announced(transferId)).containsExactly("DebitFailed"));
    assertThat(balanceOf(source)).isEqualTo(Money.of("10.00"));
  }

  @Test
  void a_compensation_refunds_the_source_and_suspense_returns_to_zero_for_that_transfer() {
    var source = walletWith("100.00");
    var transferId = UUID.randomUUID();
    deliver(new TransferRequested(
        UUID.randomUUID(), transferId, source.value(), WalletId.random().value(), "40.00",
        Instant.now()));
    await().atMost(WAIT).until(() -> announced(transferId).contains("WalletDebited"));

    deliver(new TransferCompensationRequested(
        UUID.randomUUID(), transferId, source.value(), "40.00", "WALLET_NOT_FOUND",
        Instant.now()));

    await().atMost(WAIT).untilAsserted(() ->
        assertThat(announced(transferId)).containsExactly("WalletDebited", "WalletRefunded"));
    assertThat(balanceOf(source)).isEqualTo(Money.of("100.00"));
    assertThat(suspenseBalanceOf(transferId)).isEqualByComparingTo("0.00");
  }

  @Test
  void a_top_up_is_announced_through_the_outbox() {
    var wallet = walletWith("25.00");

    var types =
        jdbc.sql("SELECT event_type FROM outbox_event WHERE partition_key = :key")
            .param("key", wallet.toString())
            .query(String.class)
            .list();

    assertThat(types).containsExactly("TopUpCompleted");
  }

  private BigDecimal suspenseBalanceOf(UUID transferId) {
    return jdbc.sql(
            """
            SELECT COALESCE(SUM(CASE e.direction WHEN 'CREDIT' THEN e.amount ELSE -e.amount END), 0)
            FROM ledger_entry e JOIN journal j ON j.id = e.journal_id
            WHERE e.account_id = :suspense AND j.reference = :transfer
            """)
        .param("suspense", SystemAccount.TRANSFER_SUSPENSE.id().toString())
        .param("transfer", transferId.toString())
        .query(BigDecimal.class)
        .single();
  }
}
