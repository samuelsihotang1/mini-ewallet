package io.github.samuelsihotang1.ewallet.wallet.domain;

import java.util.Optional;

/** Storage of completed top-ups, unique per wallet and idempotency key. */
public interface TopUpRepository {

  /** The top-up already made on {@code walletId} with {@code idempotencyKey}, if any. */
  Optional<TopUp> findByIdempotencyKey(WalletId walletId, String idempotencyKey);

  /** Stores a new top-up. */
  void save(TopUp topUp);
}
