package io.github.samuelsihotang1.ewallet.transfer.application;

import io.github.samuelsihotang1.ewallet.transfer.domain.Money;
import io.github.samuelsihotang1.ewallet.transfer.domain.Transfer;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferId;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferStatus;
import java.time.Instant;
import java.util.UUID;

/** What a client may see of a transfer. */
public record TransferView(
    TransferId id,
    UUID sourceWalletId,
    UUID destinationWalletId,
    Money amount,
    TransferStatus status,
    String failureReason,
    Instant createdAt,
    Instant updatedAt) {

  /** The view of {@code transfer} as it is now. */
  public static TransferView of(Transfer transfer) {
    return new TransferView(
        transfer.id(),
        transfer.source().walletId(),
        transfer.destination().walletId(),
        transfer.amount(),
        transfer.status(),
        transfer.failureReason(),
        transfer.createdAt(),
        transfer.updatedAt());
  }
}
