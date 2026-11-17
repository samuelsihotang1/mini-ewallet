package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.wallet.domain.JournalRepository;
import io.github.samuelsihotang1.ewallet.wallet.domain.TopUpRepository;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletRepository;
import java.time.Clock;

/** Adds money to a wallet exactly once per idempotency key. */
public class TopUpService {

  private final WalletRepository wallets;
  private final JournalRepository journals;
  private final TopUpRepository topUps;
  private final Clock clock;

  public TopUpService(
      WalletRepository wallets, JournalRepository journals, TopUpRepository topUps, Clock clock) {
    this.wallets = wallets;
    this.journals = journals;
    this.topUps = topUps;
    this.clock = clock;
  }

  public TopUpReceipt topUp(TopUpCommand command) {
    throw new UnsupportedOperationException("not implemented");
  }
}
