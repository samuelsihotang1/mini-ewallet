package io.github.samuelsihotang1.ewallet.transfer.application;

import java.util.Optional;
import java.util.UUID;

/** Asks the wallet service, synchronously, whether a wallet exists and who owns it. */
public interface WalletDirectory {

  /**
   * The owner of {@code walletId}, or empty if the wallet does not exist.
   *
   * @throws WalletDirectoryUnavailableException if the wallet service cannot answer now
   */
  Optional<String> ownerOf(UUID walletId);
}
