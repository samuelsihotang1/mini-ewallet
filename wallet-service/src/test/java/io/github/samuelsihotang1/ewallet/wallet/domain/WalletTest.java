package io.github.samuelsihotang1.ewallet.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WalletTest {

  private static Wallet walletWith(String balance) {
    return Wallet.restore(WalletId.random(), "user-1", Money.of(balance), 0);
  }

  @Test
  void a_new_wallet_starts_empty() {
    var wallet = Wallet.open(WalletId.random(), "user-1");

    assertThat(wallet.balance()).isEqualTo(Money.zero());
    assertThat(wallet.ownerId()).isEqualTo("user-1");
  }

  @Test
  void a_wallet_needs_an_owner() {
    assertThatThrownBy(() -> Wallet.open(WalletId.random(), " "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void credit_increases_the_balance() {
    var wallet = walletWith("100.00");

    wallet.credit(Money.of("50.25"));

    assertThat(wallet.balance()).isEqualTo(Money.of("150.25"));
  }

  @Test
  void debit_decreases_the_balance() {
    var wallet = walletWith("100.00");

    wallet.debit(Money.of("100.00"));

    assertThat(wallet.balance()).isEqualTo(Money.zero());
  }

  @Test
  void debit_more_than_balance_is_rejected_and_balance_is_unchanged() {
    var wallet = walletWith("100.00");

    assertThatThrownBy(() -> wallet.debit(Money.of("100.01")))
        .isInstanceOf(InsufficientBalanceException.class)
        .hasMessageContaining("100.01");
    assertThat(wallet.balance()).isEqualTo(Money.of("100.00"));
  }

  @Test
  void zero_or_negative_movements_are_rejected() {
    var wallet = walletWith("100.00");

    assertThatThrownBy(() -> wallet.credit(Money.zero()))
        .isInstanceOf(InvalidMoneyException.class);
    assertThatThrownBy(() -> wallet.debit(Money.of("-1.00")))
        .isInstanceOf(InvalidMoneyException.class);
  }
}
