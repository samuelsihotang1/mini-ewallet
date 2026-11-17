package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.wallet.domain.Wallet;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletAlreadyExistsException;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletRepository;
import org.springframework.transaction.annotation.Transactional;

/** Opens the single wallet a user may have. */
public class OpenWalletService {

  private final WalletRepository wallets;

  public OpenWalletService(WalletRepository wallets) {
    this.wallets = wallets;
  }

  /**
   * Opens an empty wallet for {@code ownerId}.
   *
   * @throws WalletAlreadyExistsException if the user already has one; a unique index on the owner
   *     backs this check when two requests race
   */
  @Transactional
  public WalletView open(String ownerId) {
    if (wallets.existsByOwner(ownerId)) {
      throw new WalletAlreadyExistsException(ownerId);
    }
    var wallet = Wallet.open(WalletId.random(), ownerId);
    wallets.save(wallet);
    return WalletView.of(wallet);
  }
}
