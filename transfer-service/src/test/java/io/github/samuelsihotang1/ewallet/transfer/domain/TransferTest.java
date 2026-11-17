package io.github.samuelsihotang1.ewallet.transfer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransferTest {

  private static final Instant T0 = Instant.parse("2026-11-17T08:00:00Z");
  private static final Instant T1 = Instant.parse("2026-11-17T08:00:01Z");
  private static final Party ALICE = new Party(UUID.randomUUID(), "alice");
  private static final Party BOB = new Party(UUID.randomUUID(), "bob");

  private static Transfer requested(String amount) {
    return Transfer.request(TransferId.random(), ALICE, BOB, Money.of(amount), "key-1", T0);
  }

  @Test
  void a_new_transfer_is_pending() {
    var transfer = requested("150000.00");

    assertThat(transfer.status()).isEqualTo(TransferStatus.PENDING);
    assertThat(transfer.source()).isEqualTo(ALICE);
    assertThat(transfer.destination()).isEqualTo(BOB);
    assertThat(transfer.amount()).isEqualTo(Money.of("150000.00"));
    assertThat(transfer.failureReason()).isNull();
    assertThat(transfer.createdAt()).isEqualTo(T0);
    assertThat(transfer.updatedAt()).isEqualTo(T0);
  }

  @Test
  void a_wallet_cannot_transfer_to_itself() {
    assertThatThrownBy(() -> Transfer.requireValid(ALICE.walletId(), ALICE.walletId(),
            Money.of("1.00")))
        .isInstanceOf(SameWalletTransferException.class);
  }

  @Test
  void zero_is_not_a_transfer() {
    assertThatThrownBy(() -> requested("0.00")).isInstanceOf(InvalidMoneyException.class);
  }

  @Test
  void a_transfer_above_the_limit_is_rejected() {
    assertThatThrownBy(() -> requested("10000000.01"))
        .isInstanceOf(TransferLimitExceededException.class);
  }

  @Test
  void a_transfer_at_the_limit_is_allowed() {
    assertThat(requested("10000000.00").amount()).isEqualTo(Transfer.MAX_AMOUNT);
  }

  @Test
  void the_happy_path_goes_pending_debited_completed() {
    var transfer = requested("10.00");

    assertThat(transfer.debited(T1)).isTrue();
    assertThat(transfer.status()).isEqualTo(TransferStatus.DEBITED);
    assertThat(transfer.credited(T1)).isTrue();

    assertThat(transfer.status()).isEqualTo(TransferStatus.COMPLETED);
    assertThat(transfer.updatedAt()).isEqualTo(T1);
  }

  @Test
  void a_failed_debit_ends_the_transfer_with_its_reason() {
    var transfer = requested("10.00");

    assertThat(transfer.debitFailed("INSUFFICIENT_BALANCE", T1)).isTrue();

    assertThat(transfer.status()).isEqualTo(TransferStatus.FAILED);
    assertThat(transfer.failureReason()).isEqualTo("INSUFFICIENT_BALANCE");
  }

  @Test
  void a_failed_credit_starts_compensation_and_a_refund_ends_it() {
    var transfer = requested("10.00");
    transfer.debited(T1);

    assertThat(transfer.creditFailed("BALANCE_LIMIT_EXCEEDED", T1)).isTrue();
    assertThat(transfer.status()).isEqualTo(TransferStatus.COMPENSATING);
    assertThat(transfer.refunded(T1)).isTrue();

    assertThat(transfer.status()).isEqualTo(TransferStatus.COMPENSATED);
    assertThat(transfer.failureReason()).isEqualTo("BALANCE_LIMIT_EXCEEDED");
  }

  @Test
  void a_repeated_or_out_of_order_step_changes_nothing() {
    var transfer = requested("10.00");
    transfer.debited(T0);
    transfer.credited(T0);

    assertThat(transfer.debited(T1)).isFalse();
    assertThat(transfer.credited(T1)).isFalse();
    assertThat(transfer.creditFailed("X", T1)).isFalse();
    assertThat(transfer.refunded(T1)).isFalse();
    assertThat(transfer.debitFailed("X", T1)).isFalse();

    assertThat(transfer.status()).isEqualTo(TransferStatus.COMPLETED);
    assertThat(transfer.updatedAt()).isEqualTo(T0);
  }

  @Test
  void a_credit_cannot_arrive_before_the_debit() {
    var transfer = requested("10.00");

    assertThat(transfer.credited(T1)).isFalse();
    assertThat(transfer.refunded(T1)).isFalse();

    assertThat(transfer.status()).isEqualTo(TransferStatus.PENDING);
  }

  @Test
  void only_completed_failed_and_compensated_are_final() {
    assertThat(TransferStatus.COMPLETED.isFinal()).isTrue();
    assertThat(TransferStatus.FAILED.isFinal()).isTrue();
    assertThat(TransferStatus.COMPENSATED.isFinal()).isTrue();
    assertThat(TransferStatus.PENDING.isFinal()).isFalse();
    assertThat(TransferStatus.DEBITED.isFinal()).isFalse();
    assertThat(TransferStatus.COMPENSATING.isFinal()).isFalse();
  }

  @Test
  void a_retry_matches_only_the_same_destination_and_amount() {
    var transfer = requested("10.00");

    assertThat(transfer.sameRequestAs(BOB.walletId(), Money.of("10"))).isTrue();
    assertThat(transfer.sameRequestAs(BOB.walletId(), Money.of("11"))).isFalse();
    assertThat(transfer.sameRequestAs(UUID.randomUUID(), Money.of("10"))).isFalse();
  }
}
