package io.github.samuelsihotang1.ewallet.events;

import java.time.Instant;
import java.util.UUID;

/** Crediting the destination failed; the wallet service should refund the source. */
public record TransferCompensationRequested(
    UUID eventId,
    UUID transferId,
    UUID sourceWalletId,
    String amount,
    String reason,
    Instant occurredAt)
    implements TransferEvent {}
