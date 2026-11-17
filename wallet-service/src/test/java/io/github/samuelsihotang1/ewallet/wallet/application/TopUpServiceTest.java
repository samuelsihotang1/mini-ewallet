package io.github.samuelsihotang1.ewallet.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.samuelsihotang1.ewallet.wallet.domain.IdempotencyKeyReusedException;
import io.github.samuelsihotang1.ewallet.wallet.domain.InvalidMoneyException;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalType;
import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.TopUpLimitExceededException;
import io.github.samuelsihotang1.ewallet.wallet.domain.Wallet;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TopUpServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-17T08:00:00Z");

  private final InMemoryStore store = new InMemoryStore();
  private final TopUpService service =
      new TopUpService(store, store, store, Clock.fixed(NOW, ZoneOffset.UTC));
  private Wallet wallet;

  @BeforeEach
  void openWallet() {
    wallet = Wallet.open(WalletId.random(), "user-1");
    store.save(wallet);
  }

  private TopUpCommand topUp(String amount, String key) {
    return new TopUpCommand(wallet.id(), Money.of(amount), key);
  }

  @Test
  void top_up_credits_the_wallet_and_posts_one_journal() {
    var receipt = service.topUp(topUp("150000", "key-1"));

    assertThat(receipt.balanceAfter()).isEqualTo(Money.of("150000"));
    assertThat(receipt.replayed()).isFalse();
    assertThat(receipt.createdAt()).isEqualTo(NOW);
    assertThat(store.wallets.get(wallet.id()).balance()).isEqualTo(Money.of("150000"));
    assertThat(store.journals)
        .singleElement()
        .satisfies(journal -> assertThat(journal.type()).isEqualTo(JournalType.TOP_UP));
  }

  @Test
  void a_retry_with_the_same_key_returns_the_first_receipt_and_credits_once() {
    var first = service.topUp(topUp("50000", "key-1"));

    var retry = service.topUp(topUp("50000", "key-1"));

    assertThat(retry.replayed()).isTrue();
    assertThat(retry.topUpId()).isEqualTo(first.topUpId());
    assertThat(retry.balanceAfter()).isEqualTo(Money.of("50000"));
    assertThat(store.wallets.get(wallet.id()).balance()).isEqualTo(Money.of("50000"));
    assertThat(store.journals).hasSize(1);
  }

  @Test
  void different_keys_are_different_top_ups() {
    service.topUp(topUp("50000", "key-1"));

    service.topUp(topUp("50000", "key-2"));

    assertThat(store.wallets.get(wallet.id()).balance()).isEqualTo(Money.of("100000"));
  }

  @Test
  void the_same_key_with_another_amount_is_rejected() {
    service.topUp(topUp("50000", "key-1"));

    assertThatThrownBy(() -> service.topUp(topUp("60000", "key-1")))
        .isInstanceOf(IdempotencyKeyReusedException.class);
    assertThat(store.wallets.get(wallet.id()).balance()).isEqualTo(Money.of("50000"));
  }

  @Test
  void a_top_up_above_the_limit_moves_nothing() {
    assertThatThrownBy(() -> service.topUp(topUp("10000000.01", "key-1")))
        .isInstanceOf(TopUpLimitExceededException.class);

    assertThat(store.journals).isEmpty();
    assertThat(store.topUps).isEmpty();
    assertThat(store.wallets.get(wallet.id()).balance()).isEqualTo(Money.zero());
  }

  @Test
  void a_top_up_at_the_limit_is_allowed() {
    var receipt = service.topUp(topUp("10000000.00", "key-1"));

    assertThat(receipt.balanceAfter()).isEqualTo(Money.of("10000000.00"));
  }

  @Test
  void a_zero_top_up_is_rejected() {
    assertThatThrownBy(() -> service.topUp(topUp("0", "key-1")))
        .isInstanceOf(InvalidMoneyException.class);
  }

  @Test
  void a_top_up_to_an_unknown_wallet_is_rejected() {
    var command = new TopUpCommand(WalletId.random(), Money.of("1000"), "key-1");

    assertThatThrownBy(() -> service.topUp(command)).isInstanceOf(WalletNotFoundException.class);
  }
}
