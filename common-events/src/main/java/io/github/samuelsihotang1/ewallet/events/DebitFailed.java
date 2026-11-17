package io.github.samuelsihotang1.ewallet.events;

import java.time.Instant;
import java.util.UUID;

/** The source wallet could not be debited; {@code reason} is a stable code such as {@code INSUFFICIENT_BALANCE}. */
public record DebitFailed(
    UUID eventId,
    UUID transferId,
    UUID walletId,
    String reason,
    Instant occurredAt)
    implements TransferStepEvent {}
