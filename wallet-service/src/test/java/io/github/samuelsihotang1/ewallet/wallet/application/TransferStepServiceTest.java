package io.github.samuelsihotang1.ewallet.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.samuelsihotang1.ewallet.events.CreditFailed;
import io.github.samuelsihotang1.ewallet.events.DebitFailed;
import io.github.samuelsihotang1.ewallet.events.WalletCredited;
import io.github.samuelsihotang1.ewallet.events.WalletDebited;
import io.github.samuelsihotang1.ewallet.events.WalletRefunded;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalType;
import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.Wallet;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransferStepServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-17T08:00:00Z");

  private final InMemoryStore store = new InMemoryStore();
  private final TransferStepService service =
      new TransferStepService(store, store, store, Clock.fixed(NOW, ZoneOffset.UTC));
  private final UUID transferId = UUID.randomUUID();

  private Wallet walletWith(String balance) {
    var wallet = Wallet.restore(WalletId.random(), "user-" + balance, Money.of(balance), 0);
    store.save(wallet);
    return wallet;
  }

  private Money balanceOf(Wallet wallet) {
    return store.wallets.get(wallet.id()).balance();
  }

  @Test
  void debit_takes_the_money_and_announces_it() {
    var source = walletWith("100.00");

    service.debit(transferId, source.id(), Money.of("30.00"));

    assertThat(balanceOf(source)).isEqualTo(Money.of("70.00"));
    assertThat(store.journals).singleElement()
        .satisfies(j -> assertThat(j.type()).isEqualTo(JournalType.TRANSFER_OUT));
    assertThat(store.published).singleElement().isInstanceOfSatisfying(WalletDebited.class, e -> {
      assertThat(e.transferId()).isEqualTo(transferId);
      assertThat(e.walletId()).isEqualTo(source.id().value());
      assertThat(e.amount()).isEqualTo("30.00");
      assertThat(e.occurredAt()).isEqualTo(NOW);
    });
  }

  @Test
  void debit_beyond_the_balance_moves_nothing_and_reports_why() {
    var source = walletWith("10.00");

    service.debit(transferId, source.id(), Money.of("30.00"));

    assertThat(balanceOf(source)).isEqualTo(Money.of("10.00"));
    assertThat(store.journals).isEmpty();
    assertThat(store.published).singleElement().isInstanceOfSatisfying(DebitFailed.class, e ->
        assertThat(e.reason()).isEqualTo("INSUFFICIENT_BALANCE"));
  }

  @Test
  void debit_from_an_unknown_wallet_is_reported_as_failed() {
    service.debit(transferId, WalletId.random(), Money.of("1.00"));

    assertThat(store.published).singleElement().isInstanceOfSatisfying(DebitFailed.class, e ->
        assertThat(e.reason()).isEqualTo("WALLET_NOT_FOUND"));
  }

  @Test
  void a_redelivered_debit_takes_the_money_once() {
    var source = walletWith("100.00");

    service.debit(transferId, source.id(), Money.of("30.00"));
    service.debit(transferId, source.id(), Money.of("30.00"));

    assertThat(balanceOf(source)).isEqualTo(Money.of("70.00"));
    assertThat(store.journals).hasSize(1);
    assertThat(store.published).hasSize(1);
  }

  @Test
  void credit_gives_the_money_and_announces_it() {
    var destination = walletWith("5.00");

    service.credit(transferId, destination.id(), Money.of("30.00"));

    assertThat(balanceOf(destination)).isEqualTo(Money.of("35.00"));
    assertThat(store.published).singleElement().isInstanceOf(WalletCredited.class);
  }

  @Test
  void credit_past_the_balance_limit_is_reported_as_failed() {
    var destination = walletWith("19999990.00");

    service.credit(transferId, destination.id(), Money.of("30.00"));

    assertThat(balanceOf(destination)).isEqualTo(Money.of("19999990.00"));
    assertThat(store.published).singleElement().isInstanceOfSatisfying(CreditFailed.class, e ->
        assertThat(e.reason()).isEqualTo("BALANCE_LIMIT_EXCEEDED"));
  }

  @Test
  void credit_to_an_unknown_wallet_is_reported_as_failed() {
    service.credit(transferId, WalletId.random(), Money.of("1.00"));

    assertThat(store.published).singleElement().isInstanceOfSatisfying(CreditFailed.class, e ->
        assertThat(e.reason()).isEqualTo("WALLET_NOT_FOUND"));
  }

  @Test
  void a_redelivered_credit_gives_the_money_once() {
    var destination = walletWith("5.00");

    service.credit(transferId, destination.id(), Money.of("30.00"));
    service.credit(transferId, destination.id(), Money.of("30.00"));

    assertThat(balanceOf(destination)).isEqualTo(Money.of("35.00"));
    assertThat(store.published).hasSize(1);
  }

  @Test
  void refund_returns_the_money_once() {
    var source = walletWith("100.00");
    service.debit(transferId, source.id(), Money.of("30.00"));

    service.refund(transferId, source.id(), Money.of("30.00"));
    service.refund(transferId, source.id(), Money.of("30.00"));

    assertThat(balanceOf(source)).isEqualTo(Money.of("100.00"));
    assertThat(store.published).last().isInstanceOf(WalletRefunded.class);
    assertThat(store.published).hasSize(2);
  }

  @Test
  void a_refund_to_a_vanished_wallet_is_a_bug_that_must_be_retried() {
    assertThatThrownBy(() -> service.refund(transferId, WalletId.random(), Money.of("1.00")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void suspense_is_empty_after_a_transfer_completes_or_is_refunded() {
    var source = walletWith("100.00");
    var destination = walletWith("0.01");
    var refunded = UUID.randomUUID();

    service.debit(transferId, source.id(), Money.of("30.00"));
    service.credit(transferId, destination.id(), Money.of("30.00"));
    service.debit(refunded, source.id(), Money.of("20.00"));
    service.refund(refunded, source.id(), Money.of("20.00"));

    assertThat(store.suspenseBalance()).isEqualTo(Money.zero());
    assertThat(balanceOf(source)).isEqualTo(Money.of("70.00"));
  }
}
