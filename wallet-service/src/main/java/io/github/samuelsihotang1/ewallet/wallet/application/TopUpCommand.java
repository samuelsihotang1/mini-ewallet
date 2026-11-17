package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;

/** A request to add money to a wallet; {@code idempotencyKey} makes retries safe. */
public record TopUpCommand(WalletId walletId, Money amount, String idempotencyKey) {}
