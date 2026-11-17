package io.github.samuelsihotang1.ewallet.transfer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.samuelsihotang1.ewallet.events.TransferRequested;
import io.github.samuelsihotang1.ewallet.transfer.domain.IdempotencyKeyReusedException;
import io.github.samuelsihotang1.ewallet.transfer.domain.Money;
import io.github.samuelsihotang1.ewallet.transfer.domain.SameWalletTransferException;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferStatus;
import io.github.samuelsihotang1.ewallet.transfer.domain.WalletNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

class RequestTransferServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-17T08:00:00.123456789Z");

  private final InMemoryTransfers store = new InMemoryTransfers();
  private final RequestTransferService service =
      new RequestTransferService(
          store, store, store, TransactionOperations.withoutTransaction(),
          Clock.fixed(NOW, ZoneOffset.UTC));
  private final UUID alice = UUID.randomUUID();
  private final UUID bob = UUID.randomUUID();

  @BeforeEach
  void knownWallets() {
    store.owners.put(alice, "user-alice");
    store.owners.put(bob, "user-bob");
  }

  private RequestTransferCommand transfer(String amount, String key) {
    return new RequestTransferCommand(alice, bob, Money.of(amount), key);
  }

  @Test
  void a_transfer_is_stored_pending_and_requested_from_the_wallet_service() {
    var receipt = service.request(transfer("150000", "key-1"));

    assertThat(receipt.replayed()).isFalse();
    assertThat(receipt.transfer().status()).isEqualTo(TransferStatus.PENDING);
    assertThat(receipt.transfer().createdAt()).isEqualTo(Instant.parse("2026-11-17T08:00:00.123456Z"));
    assertThat(store.transfers).hasSize(1);
    assertThat(store.published).singleElement().isInstanceOfSatisfying(TransferRequested.class,
        e -> {
          assertThat(e.transferId()).isEqualTo(receipt.transfer().id().value());
          assertThat(e.sourceWalletId()).isEqualTo(alice);
          assertThat(e.destinationWalletId()).isEqualTo(bob);
          assertThat(e.amount()).isEqualTo("150000.00");
        });
  }

  @Test
  void the_owners_are_captured_for_the_notifications() {
    var receipt = service.request(transfer("1", "key-1"));

    var stored = store.transfers.get(receipt.transfer().id());
    assertThat(stored.source().ownerId()).isEqualTo("user-alice");
    assertThat(stored.destination().ownerId()).isEqualTo("user-bob");
  }

  @Test
  void a_retry_with_the_same_key_returns_the_first_transfer_and_requests_it_once() {
    var first = service.request(transfer("50000", "key-1"));

    var retry = service.request(transfer("50000", "key-1"));

    assertThat(retry.replayed()).isTrue();
    assertThat(retry.transfer().id()).isEqualTo(first.transfer().id());
    assertThat(store.published).hasSize(1);
  }

  @Test
  void the_same_key_with_another_amount_is_rejected() {
    service.request(transfer("50000", "key-1"));

    assertThatThrownBy(() -> service.request(transfer("60000", "key-1")))
        .isInstanceOf(IdempotencyKeyReusedException.class);
  }

  @Test
  void an_unknown_destination_is_rejected_and_nothing_is_stored() {
    var command = new RequestTransferCommand(alice, UUID.randomUUID(), Money.of("1"), "key-1");

    assertThatThrownBy(() -> service.request(command))
        .isInstanceOf(WalletNotFoundException.class);
    assertThat(store.transfers).isEmpty();
    assertThat(store.published).isEmpty();
  }

  @Test
  void an_unknown_source_is_rejected() {
    var command = new RequestTransferCommand(UUID.randomUUID(), bob, Money.of("1"), "key-1");

    assertThatThrownBy(() -> service.request(command))
        .isInstanceOf(WalletNotFoundException.class);
  }

  @Test
  void a_transfer_to_the_same_wallet_is_rejected_before_asking_the_wallet_service() {
    var command = new RequestTransferCommand(alice, alice, Money.of("1"), "key-1");

    assertThatThrownBy(() -> service.request(command))
        .isInstanceOf(SameWalletTransferException.class);
    assertThat(store.directoryCalls).isZero();
  }

  @Test
  void when_the_wallet_service_is_down_nothing_is_stored() {
    store.directoryDown = true;

    assertThatThrownBy(() -> service.request(transfer("1", "key-1")))
        .isInstanceOf(WalletDirectoryUnavailableException.class);
    assertThat(store.transfers).isEmpty();
  }

  @Test
  void a_replay_does_not_need_the_wallet_service() {
    service.request(transfer("1", "key-1"));
    store.directoryDown = true;

    assertThat(service.request(transfer("1", "key-1")).replayed()).isTrue();
  }
}
