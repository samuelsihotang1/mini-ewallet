package io.github.samuelsihotang1.ewallet.events;

import java.time.Instant;
import java.util.UUID;

/** The transfer ended without moving money; {@code compensated} is true when a debit was refunded. */
public record TransferFailed(
    UUID eventId,
    UUID transferId,
    UUID sourceWalletId,
    String sourceUserId,
    UUID destinationWalletId,
    String destinationUserId,
    String amount,
    String reason,
    boolean compensated,
    Instant occurredAt)
    implements TransferEvent {}
