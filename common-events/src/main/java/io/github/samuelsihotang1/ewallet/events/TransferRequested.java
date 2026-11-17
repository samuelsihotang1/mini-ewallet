package io.github.samuelsihotang1.ewallet.events;

import java.time.Instant;
import java.util.UUID;

/** A transfer was accepted; the wallet service should debit the source. Amounts are IDR text with two decimals. */
public record TransferRequested(
    UUID eventId,
    UUID transferId,
    UUID sourceWalletId,
    UUID destinationWalletId,
    String amount,
    Instant occurredAt)
    implements TransferEvent {}
