package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.wallet.domain.WalletRepository;

/** Opens the single wallet a user may have. */
public class OpenWalletService {

  private final WalletRepository wallets;

  public OpenWalletService(WalletRepository wallets) {
    this.wallets = wallets;
  }

  public WalletView open(String ownerId) {
    throw new UnsupportedOperationException("not implemented");
  }
}
