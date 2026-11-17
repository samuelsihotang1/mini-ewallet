package io.github.samuelsihotang1.ewallet.events;

import java.time.Instant;
import java.util.UUID;

/** The suspended money went back to the source wallet. */
public record WalletRefunded(
    UUID eventId,
    UUID transferId,
    UUID walletId,
    String amount,
    Instant occurredAt)
    implements TransferStepEvent {}
