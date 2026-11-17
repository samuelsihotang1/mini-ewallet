package io.github.samuelsihotang1.ewallet.transfer.application;

import io.github.samuelsihotang1.ewallet.transfer.domain.Money;
import java.util.UUID;

/** A request to move {@code amount} from one wallet to another, once per idempotency key. */
public record RequestTransferCommand(
    UUID sourceWalletId, UUID destinationWalletId, Money amount, String idempotencyKey) {}
