package io.github.samuelsihotang1.ewallet.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.samuelsihotang1.ewallet.wallet.domain.LedgerEntry.Direction;
import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.Wallet;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class WalletQueryServiceTest {

  private final InMemoryStore store = new InMemoryStore();
  private final WalletQueryService queries = new WalletQueryService(store, store);

  @Test
  void shows_the_balance_of_a_wallet() {
    var wallet = Wallet.open(WalletId.random(), "user-1");
    store.save(wallet);

    var view = queries.get(wallet.id());

    assertThat(view.id()).isEqualTo(wallet.id());
    assertThat(view.balance()).isEqualTo(Money.zero());
  }

  @Test
  void an_unknown_wallet_is_not_found() {
    assertThatThrownBy(() -> queries.get(WalletId.random()))
        .isInstanceOf(WalletNotFoundException.class);
  }

  @Test
  void history_lists_the_newest_mutation_first() {
    var wallet = Wallet.open(WalletId.random(), "user-1");
    store.save(wallet);
    topUpAt(wallet, "1000", "key-1", "2026-11-17T08:00:00Z");
    topUpAt(wallet, "2000", "key-2", "2026-11-17T09:00:00Z");

    var history = queries.mutations(wallet.id(), 0, 10);

    assertThat(history)
        .extracting(Mutation::amount)
        .containsExactly(Money.of("2000"), Money.of("1000"));
    assertThat(history).extracting(Mutation::direction).containsOnly(Direction.CREDIT);
    assertThat(history.getFirst().balanceAfter()).isEqualTo(Money.of("3000"));
  }

  @Test
  void history_of_an_unknown_wallet_is_not_found() {
    assertThatThrownBy(() -> queries.mutations(WalletId.random(), 0, 10))
        .isInstanceOf(WalletNotFoundException.class);
  }

  @Test
  void history_page_bounds_are_checked() {
    var wallet = Wallet.open(WalletId.random(), "user-1");
    store.save(wallet);

    assertThatThrownBy(() -> queries.mutations(wallet.id(), 0, 101))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> queries.mutations(wallet.id(), -1, 10))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private void topUpAt(Wallet wallet, String amount, String key, String at) {
    var clock = Clock.fixed(Instant.parse(at), ZoneOffset.UTC);
    new TopUpService(store, store, store, store, clock)
        .topUp(new TopUpCommand(wallet.id(), Money.of(amount), key));
  }
}
