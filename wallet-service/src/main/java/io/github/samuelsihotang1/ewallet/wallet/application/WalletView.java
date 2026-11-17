package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.Wallet;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;

/** A wallet as shown to its owner. */
public record WalletView(WalletId id, String ownerId, Money balance) {

  static WalletView of(Wallet wallet) {
    return new WalletView(wallet.id(), wallet.ownerId(), wallet.balance());
  }
}
