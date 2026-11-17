package io.github.samuelsihotang1.ewallet.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.samuelsihotang1.ewallet.wallet.domain.LedgerEntry.Direction;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TransferJournalTest {

  private static final Instant NOW = Instant.parse("2026-11-17T08:00:00Z");
  private static final WalletId SUSPENSE = SystemAccount.TRANSFER_SUSPENSE.id();

  private static Wallet walletWith(String balance) {
    return Wallet.restore(WalletId.random(), "user-1", Money.of(balance), 0);
  }

  @Test
  void transfer_out_moves_money_from_the_source_into_suspense() {
    var source = walletWith("100.00");

    var journal = Journal.transferOut(JournalId.random(), source, Money.of("40.00"), "t-1", NOW);

    assertThat(source.balance()).isEqualTo(Money.of("60.00"));
    assertThat(journal.type()).isEqualTo(JournalType.TRANSFER_OUT);
    assertThat(journal.reference()).isEqualTo("t-1");
    assertThat(journal.entries())
        .extracting(LedgerEntry::accountId, LedgerEntry::direction, LedgerEntry::balanceAfter)
        .containsExactly(
            tuple(source.id(), Direction.DEBIT, Money.of("60.00")),
            tuple(SUSPENSE, Direction.CREDIT, null));
  }

  @Test
  void transfer_out_beyond_the_balance_posts_nothing() {
    var source = walletWith("10.00");

    assertThatThrownBy(
            () -> Journal.transferOut(JournalId.random(), source, Money.of("10.01"), "t-1", NOW))
        .isInstanceOf(InsufficientBalanceException.class);
    assertThat(source.balance()).isEqualTo(Money.of("10.00"));
  }

  @Test
  void transfer_in_moves_money_from_suspense_to_the_destination() {
    var destination = walletWith("5.00");

    var journal =
        Journal.transferIn(JournalId.random(), destination, Money.of("40.00"), "t-1", NOW);

    assertThat(destination.balance()).isEqualTo(Money.of("45.00"));
    assertThat(journal.type()).isEqualTo(JournalType.TRANSFER_IN);
    assertThat(journal.entries())
        .extracting(LedgerEntry::accountId, LedgerEntry::direction)
        .containsExactly(
            tuple(SUSPENSE, Direction.DEBIT), tuple(destination.id(), Direction.CREDIT));
  }

  @Test
  void transfer_in_past_the_balance_limit_is_rejected() {
    var destination = walletWith("19999999.00");

    assertThatThrownBy(
            () ->
                Journal.transferIn(
                    JournalId.random(), destination, Money.of("1.01"), "t-1", NOW))
        .isInstanceOf(BalanceLimitExceededException.class);
  }

  @Test
  void a_refund_returns_suspended_money_even_past_the_balance_limit() {
    var source = walletWith("19999999.00");

    var journal =
        Journal.transferRefund(JournalId.random(), source, Money.of("40.00"), "t-1", NOW);

    assertThat(source.balance()).isEqualTo(Money.of("20000039.00"));
    assertThat(journal.type()).isEqualTo(JournalType.TRANSFER_REFUND);
    assertThat(journal.entries())
        .extracting(LedgerEntry::accountId, LedgerEntry::direction)
        .containsExactly(tuple(SUSPENSE, Direction.DEBIT), tuple(source.id(), Direction.CREDIT));
  }
}
