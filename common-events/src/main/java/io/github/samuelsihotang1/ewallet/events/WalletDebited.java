package io.github.samuelsihotang1.ewallet.events;

import java.time.Instant;
import java.util.UUID;

/** The source wallet was debited into the transfer suspense account. */
public record WalletDebited(
    UUID eventId,
    UUID transferId,
    UUID walletId,
    String amount,
    Instant occurredAt)
    implements TransferStepEvent {}
