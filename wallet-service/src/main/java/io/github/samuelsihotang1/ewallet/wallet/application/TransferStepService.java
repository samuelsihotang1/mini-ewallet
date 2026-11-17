package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.wallet.domain.JournalRepository;
import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletRepository;
import java.time.Clock;
import java.util.UUID;

/** The wallet's steps of the transfer saga. */
public class TransferStepService {

  public TransferStepService(
      WalletRepository wallets, JournalRepository journals, WalletEvents events, Clock clock) {}

  public void debit(UUID transferId, WalletId sourceId, Money amount) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void credit(UUID transferId, WalletId destinationId, Money amount) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void refund(UUID transferId, WalletId sourceId, Money amount) {
    throw new UnsupportedOperationException("not implemented");
  }
}
