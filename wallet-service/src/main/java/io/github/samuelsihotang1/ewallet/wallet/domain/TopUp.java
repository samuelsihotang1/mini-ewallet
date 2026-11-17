package io.github.samuelsihotang1.ewallet.wallet.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A completed top-up, remembered by its idempotency key so a retried request returns this result
 * instead of crediting the wallet again.
 *
 * @param balanceAfter the wallet balance right after this top-up, returned again on a replay
 */
public record TopUp(
    UUID id,
    WalletId walletId,
    String idempotencyKey,
    Money amount,
    JournalId journalId,
    Money balanceAfter,
    Instant createdAt) {

  /** The largest single top-up. */
  public static final Money MAX_AMOUNT = Money.of("10000000.00");

  public TopUp {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(walletId, "wallet id");
    Objects.requireNonNull(idempotencyKey, "idempotency key");
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(journalId, "journal id");
    Objects.requireNonNull(balanceAfter, "balance after");
    Objects.requireNonNull(createdAt, "created at");
  }

  /**
   * Checks a requested top-up amount against the rules before any money moves.
   *
   * @throws InvalidMoneyException if the amount is not positive
   * @throws TopUpLimitExceededException if it is above {@link #MAX_AMOUNT}
   */
  public static void requireAllowed(Money amount) {
    if (!amount.isPositive()) {
      throw new InvalidMoneyException("top-up amount must be positive, was " + amount);
    }
    if (amount.isGreaterThan(MAX_AMOUNT)) {
      throw new TopUpLimitExceededException(amount, MAX_AMOUNT);
    }
  }

  /** Whether a retried request with the same key asks for exactly this top-up. */
  public boolean sameRequestAs(WalletId otherWallet, Money otherAmount) {
    return walletId.equals(otherWallet) && amount.equals(otherAmount);
  }
}
