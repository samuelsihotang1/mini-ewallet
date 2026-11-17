package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.TopUp;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import java.time.Instant;
import java.util.UUID;

/**
 * The result of a top-up.
 *
 * @param replayed true when this is the stored result of an earlier request with the same key
 */
public record TopUpReceipt(
    UUID topUpId,
    WalletId walletId,
    Money amount,
    Money balanceAfter,
    Instant createdAt,
    boolean replayed) {

  static TopUpReceipt of(TopUp topUp, boolean replayed) {
    return new TopUpReceipt(
        topUp.id(),
        topUp.walletId(),
        topUp.amount(),
        topUp.balanceAfter(),
        topUp.createdAt(),
        replayed);
  }
}
