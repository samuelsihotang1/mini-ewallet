package io.github.samuelsihotang1.ewallet.events;

import java.time.Instant;
import java.util.UUID;

/** The destination wallet was credited from the transfer suspense account. */
public record WalletCredited(
    UUID eventId,
    UUID transferId,
    UUID walletId,
    String amount,
    Instant occurredAt)
    implements TransferStepEvent {}
