package io.github.samuelsihotang1.ewallet.transfer.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.samuelsihotang1.ewallet.events.CreditFailed;
import io.github.samuelsihotang1.ewallet.events.DebitFailed;
import io.github.samuelsihotang1.ewallet.events.EventCodec;
import io.github.samuelsihotang1.ewallet.events.Topics;
import io.github.samuelsihotang1.ewallet.events.WalletCredited;
import io.github.samuelsihotang1.ewallet.events.WalletDebited;
import io.github.samuelsihotang1.ewallet.events.WalletEvent;
import io.github.samuelsihotang1.ewallet.events.WalletRefunded;
import io.github.samuelsihotang1.ewallet.transfer.TestcontainersConfiguration;
import io.github.samuelsihotang1.ewallet.transfer.domain.Money;
import io.github.samuelsihotang1.ewallet.transfer.domain.Party;
import io.github.samuelsihotang1.ewallet.transfer.domain.Transfer;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferId;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferRepository;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** The transfer service's side of the saga, driven through real Kafka and MySQL. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WalletEventListenerIT {

  private static final Duration WAIT = Duration.ofSeconds(30);

  @Autowired TransferRepository transfers;
  @Autowired KafkaTemplate<String, String> kafka;
  @Autowired EventCodec codec;
  @Autowired JdbcClient jdbc;
  @Autowired TransactionTemplate transaction;

  private Transfer pending() {
    var transfer = Transfer.request(
        TransferId.random(),
        new Party(UUID.randomUUID(), "user-alice"),
        new Party(UUID.randomUUID(), "user-bob"),
        Money.of("40.00"),
        "key-" + UUID.randomUUID(),
        Instant.now().truncatedTo(ChronoUnit.MICROS));
    transaction.executeWithoutResult(status -> transfers.add(transfer));
    return transfer;
  }

  private void deliver(WalletEvent event) {
    kafka.send(Topics.WALLET_EVENTS, event.partitionKey(), codec.encode(event)).join();
  }

  private TransferStatus statusOf(Transfer transfer) {
    return transfers.findById(transfer.id()).orElseThrow().status();
  }

  private List<String> announced(Transfer transfer) {
    return jdbc.sql("SELECT event_type FROM outbox_event WHERE partition_key = :key ORDER BY seq")
        .param("key", transfer.id().toString())
        .query(String.class)
        .list();
  }

  @Test
  void a_debit_delivered_twice_moves_the_transfer_on_once() {
    var transfer = pending();
    var debited = new WalletDebited(
        UUID.randomUUID(), transfer.id().value(), transfer.source().walletId(), "40.00",
        Instant.now());

    deliver(debited);
    deliver(debited);

    await().atMost(WAIT).untilAsserted(() ->
        assertThat(announced(transfer)).containsExactly("TransferDebited"));
    assertThat(statusOf(transfer)).isEqualTo(TransferStatus.DEBITED);
  }

  @Test
  void the_whole_happy_path_completes_the_transfer() {
    var transfer = pending();
    var id = transfer.id().value();

    deliver(new WalletDebited(UUID.randomUUID(), id, UUID.randomUUID(), "40.00", Instant.now()));
    await().atMost(WAIT).until(() -> statusOf(transfer) == TransferStatus.DEBITED);
    deliver(new WalletCredited(UUID.randomUUID(), id, UUID.randomUUID(), "40.00", Instant.now()));

    await().atMost(WAIT).untilAsserted(() ->
        assertThat(announced(transfer)).containsExactly("TransferDebited", "TransferCompleted"));
    assertThat(statusOf(transfer)).isEqualTo(TransferStatus.COMPLETED);
  }

  @Test
  void a_failed_debit_fails_the_transfer() {
    var transfer = pending();

    deliver(new DebitFailed(
        UUID.randomUUID(), transfer.id().value(), UUID.randomUUID(), "INSUFFICIENT_BALANCE",
        Instant.now()));

    await().atMost(WAIT).untilAsserted(() ->
        assertThat(announced(transfer)).containsExactly("TransferFailed"));
    assertThat(statusOf(transfer)).isEqualTo(TransferStatus.FAILED);
  }

  @Test
  void a_failed_credit_is_compensated_by_a_refund() {
    var transfer = pending();
    var id = transfer.id().value();
    deliver(new WalletDebited(UUID.randomUUID(), id, UUID.randomUUID(), "40.00", Instant.now()));
    await().atMost(WAIT).until(() -> statusOf(transfer) == TransferStatus.DEBITED);

    deliver(new CreditFailed(
        UUID.randomUUID(), id, UUID.randomUUID(), "BALANCE_LIMIT_EXCEEDED", Instant.now()));
    await().atMost(WAIT).until(() -> statusOf(transfer) == TransferStatus.COMPENSATING);
    deliver(new WalletRefunded(UUID.randomUUID(), id, UUID.randomUUID(), "40.00", Instant.now()));

    await().atMost(WAIT).untilAsserted(() -> assertThat(announced(transfer)).containsExactly(
        "TransferDebited", "TransferCompensationRequested", "TransferFailed"));
    assertThat(statusOf(transfer)).isEqualTo(TransferStatus.COMPENSATED);
  }
}
