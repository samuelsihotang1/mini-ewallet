package io.github.samuelsihotang1.ewallet.events;

import java.time.Instant;
import java.util.UUID;

/** The money reached the destination; carries both owners so notifications need no lookup. */
public record TransferCompleted(
    UUID eventId,
    UUID transferId,
    UUID sourceWalletId,
    String sourceUserId,
    UUID destinationWalletId,
    String destinationUserId,
    String amount,
    Instant occurredAt)
    implements TransferEvent {}
