package io.github.samuelsihotang1.ewallet.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.samuelsihotang1.ewallet.wallet.domain.LedgerEntry.Direction;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class JournalTest {

  private static final Instant NOW = Instant.parse("2026-11-17T08:00:00Z");

  @Test
  void top_up_debits_the_top_up_source_and_credits_the_wallet() {
    var wallet = Wallet.restore(WalletId.random(), "user-1", Money.of("10.00"), 0);

    var journal = Journal.topUp(JournalId.random(), wallet, Money.of("150000"), "topup-1", NOW);

    assertThat(wallet.balance()).isEqualTo(Money.of("150010.00"));
    assertThat(journal.type()).isEqualTo(JournalType.TOP_UP);
    assertThat(journal.entries())
        .extracting(LedgerEntry::accountId, LedgerEntry::direction, LedgerEntry::amount)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(
                SystemAccount.TOP_UP_SOURCE.id(), Direction.DEBIT, Money.of("150000")),
            org.assertj.core.groups.Tuple.tuple(
                wallet.id(), Direction.CREDIT, Money.of("150000")));
  }

  @Test
  void the_wallet_entry_records_the_balance_after_it() {
    var wallet = Wallet.restore(WalletId.random(), "user-1", Money.of("10.00"), 0);

    var journal = Journal.topUp(JournalId.random(), wallet, Money.of("5.00"), "topup-1", NOW);

    assertThat(journal.entryFor(wallet.id()).balanceAfter()).isEqualTo(Money.of("15.00"));
    assertThat(journal.entryFor(SystemAccount.TOP_UP_SOURCE.id()).balanceAfter()).isNull();
  }

  @Test
  void every_journal_balances_debits_against_credits() {
    var wallet = Wallet.open(WalletId.random(), "user-1");

    var journal = Journal.topUp(JournalId.random(), wallet, Money.of("99.99"), "topup-1", NOW);

    assertThat(journal.total(Direction.DEBIT)).isEqualTo(journal.total(Direction.CREDIT));
  }

  @Test
  void an_unbalanced_journal_cannot_exist() {
    var walletId = WalletId.random();
    var unbalanced =
        List.of(
            new LedgerEntry(walletId, Direction.CREDIT, Money.of("10.00"), Money.of("10.00")),
            new LedgerEntry(
                SystemAccount.TOP_UP_SOURCE.id(), Direction.DEBIT, Money.of("9.99"), null));

    assertThatThrownBy(
            () -> new Journal(JournalId.random(), JournalType.TOP_UP, "ref", NOW, unbalanced))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("unbalanced");
  }

  @Test
  void a_journal_needs_at_least_two_entries() {
    var single =
        List.of(new LedgerEntry(WalletId.random(), Direction.CREDIT, Money.of("1.00"), null));

    assertThatThrownBy(
            () -> new Journal(JournalId.random(), JournalType.TOP_UP, "ref", NOW, single))
        .isInstanceOf(IllegalStateException.class);
  }
}
