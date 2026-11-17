package io.github.samuelsihotang1.ewallet.wallet.domain;

import io.github.samuelsihotang1.ewallet.wallet.domain.LedgerEntry.Direction;
import java.time.Instant;
import java.util.List;

/** One balanced posting to the ledger. */
public record Journal(
    JournalId id, JournalType type, String reference, Instant postedAt, List<LedgerEntry> entries) {

  public static Journal topUp(
      JournalId id, Wallet wallet, Money amount, String reference, Instant postedAt) {
    throw new UnsupportedOperationException("not implemented");
  }

  public Money total(Direction direction) {
    throw new UnsupportedOperationException("not implemented");
  }

  public LedgerEntry entryFor(WalletId accountId) {
    throw new UnsupportedOperationException("not implemented");
  }
}
