package io.github.samuelsihotang1.ewallet.wallet.domain;

import java.util.Objects;

/**
 * One line of a {@link Journal}: an amount debited from or credited to one account.
 *
 * @param balanceAfter the customer wallet's balance right after this entry, or {@code null} for a
 *     {@link SystemAccount}, which keeps no balance
 */
public record LedgerEntry(
    WalletId accountId, Direction direction, Money amount, Money balanceAfter) {

  /** Debit takes money out of a customer wallet; credit puts money in. */
  public enum Direction {
    DEBIT,
    CREDIT
  }

  public LedgerEntry {
    Objects.requireNonNull(accountId, "account id");
    Objects.requireNonNull(direction, "direction");
    Objects.requireNonNull(amount, "amount");
    if (!amount.isPositive()) {
      throw new InvalidMoneyException("ledger entry amount must be positive, was " + amount);
    }
  }
}
