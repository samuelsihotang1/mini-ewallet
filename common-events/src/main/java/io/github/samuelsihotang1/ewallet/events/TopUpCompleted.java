package io.github.samuelsihotang1.ewallet.events;

import java.time.Instant;
import java.util.UUID;

/** Money arrived in a wallet from outside; keyed by the wallet. */
public record TopUpCompleted(
    UUID eventId,
    UUID walletId,
    String userId,
    String amount,
    String balanceAfter,
    Instant occurredAt)
    implements WalletEvent {

  @Override
  public String partitionKey() {
    return walletId.toString();
  }
}
