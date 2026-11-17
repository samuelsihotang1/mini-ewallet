package io.github.samuelsihotang1.ewallet.transfer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.samuelsihotang1.ewallet.events.TransferCompensationRequested;
import io.github.samuelsihotang1.ewallet.events.TransferCompleted;
import io.github.samuelsihotang1.ewallet.events.TransferDebited;
import io.github.samuelsihotang1.ewallet.events.TransferFailed;
import io.github.samuelsihotang1.ewallet.transfer.domain.Money;
import io.github.samuelsihotang1.ewallet.transfer.domain.Party;
import io.github.samuelsihotang1.ewallet.transfer.domain.Transfer;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferId;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransferSagaServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-17T08:00:00Z");

  private final InMemoryTransfers store = new InMemoryTransfers();
  private final TransferSagaService saga =
      new TransferSagaService(store, store, Clock.fixed(NOW, ZoneOffset.UTC));
  private final Party alice = new Party(UUID.randomUUID(), "user-alice");
  private final Party bob = new Party(UUID.randomUUID(), "user-bob");
  private Transfer transfer;

  @BeforeEach
  void pendingTransfer() {
    transfer = Transfer.request(TransferId.random(), alice, bob, Money.of("40.00"), "key-1", NOW);
    store.save(transfer);
  }

  private UUID id() {
    return transfer.id().value();
  }

  private TransferStatus status() {
    return store.transfers.get(transfer.id()).status();
  }

  @Test
  void a_debit_asks_the_wallet_service_to_credit_the_destination() {
    saga.onDebited(id());

    assertThat(status()).isEqualTo(TransferStatus.DEBITED);
    assertThat(store.published).singleElement().isInstanceOfSatisfying(TransferDebited.class,
        e -> {
          assertThat(e.transferId()).isEqualTo(id());
          assertThat(e.destinationWalletId()).isEqualTo(bob.walletId());
          assertThat(e.amount()).isEqualTo("40.00");
        });
  }

  @Test
  void a_credit_completes_the_transfer_and_names_both_owners() {
    saga.onDebited(id());

    saga.onCredited(id());

    assertThat(status()).isEqualTo(TransferStatus.COMPLETED);
    assertThat(store.published).last().isInstanceOfSatisfying(TransferCompleted.class, e -> {
      assertThat(e.sourceUserId()).isEqualTo("user-alice");
      assertThat(e.destinationUserId()).isEqualTo("user-bob");
      assertThat(e.amount()).isEqualTo("40.00");
    });
  }

  @Test
  void a_failed_debit_fails_the_transfer_without_compensation() {
    saga.onDebitFailed(id(), "INSUFFICIENT_BALANCE");

    assertThat(status()).isEqualTo(TransferStatus.FAILED);
    assertThat(store.published).singleElement().isInstanceOfSatisfying(TransferFailed.class,
        e -> {
          assertThat(e.reason()).isEqualTo("INSUFFICIENT_BALANCE");
          assertThat(e.compensated()).isFalse();
        });
  }

  @Test
  void a_failed_credit_asks_for_a_refund_of_the_source() {
    saga.onDebited(id());

    saga.onCreditFailed(id(), "BALANCE_LIMIT_EXCEEDED");

    assertThat(status()).isEqualTo(TransferStatus.COMPENSATING);
    assertThat(store.published).last()
        .isInstanceOfSatisfying(TransferCompensationRequested.class, e -> {
          assertThat(e.sourceWalletId()).isEqualTo(alice.walletId());
          assertThat(e.amount()).isEqualTo("40.00");
          assertThat(e.reason()).isEqualTo("BALANCE_LIMIT_EXCEEDED");
        });
  }

  @Test
  void a_refund_ends_the_transfer_as_compensated_with_the_original_reason() {
    saga.onDebited(id());
    saga.onCreditFailed(id(), "BALANCE_LIMIT_EXCEEDED");

    saga.onRefunded(id());

    assertThat(status()).isEqualTo(TransferStatus.COMPENSATED);
    assertThat(store.published).last().isInstanceOfSatisfying(TransferFailed.class, e -> {
      assertThat(e.reason()).isEqualTo("BALANCE_LIMIT_EXCEEDED");
      assertThat(e.compensated()).isTrue();
    });
  }

  @Test
  void a_repeated_step_publishes_nothing_more() {
    saga.onDebited(id());
    saga.onDebited(id());
    saga.onCredited(id());
    saga.onCredited(id());

    assertThat(store.published).hasSize(2);
  }

  @Test
  void an_answer_for_an_unknown_transfer_is_a_bug() {
    assertThatThrownBy(() -> saga.onDebited(UUID.randomUUID()))
        .isInstanceOf(IllegalStateException.class);
  }
}
