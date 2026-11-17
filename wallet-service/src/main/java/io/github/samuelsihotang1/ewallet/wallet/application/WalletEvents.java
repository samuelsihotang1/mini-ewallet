package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.events.WalletEvent;

/** Where the wallet announces what happened to money, for the other services. */
public interface WalletEvents {

  /**
   * Publishes {@code event} if and only if the caller's transaction commits.
   *
   * @throws IllegalStateException when called outside a transaction
   */
  void publish(WalletEvent event);
}
