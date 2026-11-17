package io.github.samuelsihotang1.ewallet.events;

import java.time.Instant;
import java.util.UUID;

/** The source was debited; the wallet service should credit the destination. */
public record TransferDebited(
    UUID eventId,
    UUID transferId,
    UUID destinationWalletId,
    String amount,
    Instant occurredAt)
    implements TransferEvent {}
