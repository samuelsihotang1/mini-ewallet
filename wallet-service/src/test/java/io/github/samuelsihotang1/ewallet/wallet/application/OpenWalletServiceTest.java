package io.github.samuelsihotang1.ewallet.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletAlreadyExistsException;
import org.junit.jupiter.api.Test;

class OpenWalletServiceTest {

  private final InMemoryStore store = new InMemoryStore();
  private final OpenWalletService service = new OpenWalletService(store);

  @Test
  void opens_an_empty_wallet_for_a_user() {
    var wallet = service.open("user-1");

    assertThat(wallet.ownerId()).isEqualTo("user-1");
    assertThat(wallet.balance()).isEqualTo(Money.zero());
    assertThat(store.wallets).containsKey(wallet.id());
  }

  @Test
  void a_user_cannot_open_a_second_wallet() {
    service.open("user-1");

    assertThatThrownBy(() -> service.open("user-1"))
        .isInstanceOf(WalletAlreadyExistsException.class);
    assertThat(store.wallets).hasSize(1);
  }
}
