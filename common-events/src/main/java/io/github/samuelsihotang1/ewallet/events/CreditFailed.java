package io.github.samuelsihotang1.ewallet.events;

import java.time.Instant;
import java.util.UUID;

/** The destination wallet could not be credited; the money is still in suspense. */
public record CreditFailed(
    UUID eventId,
    UUID transferId,
    UUID walletId,
    String reason,
    Instant occurredAt)
    implements TransferStepEvent {}
