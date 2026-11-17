package io.github.samuelsihotang1.ewallet.transfer.application;

/** The answer to a transfer request; {@code replayed} is true for a retry of an earlier one. */
public record TransferReceipt(TransferView transfer, boolean replayed) {}
