package io.github.samuelsihotang1.ewallet.wallet.domain;

import java.util.Optional;

/** Storage of {@link Wallet} aggregates. */
public interface WalletRepository {

  /** The wallet, read without a lock. */
  Optional<Wallet> findById(WalletId id);

  /**
   * The wallet, locked for update until the current transaction ends, so concurrent money
   * movements on it run one after another.
   */
  Optional<Wallet> lockById(WalletId id);

  /** Whether {@code ownerId} already has a wallet. */
  boolean existsByOwner(String ownerId);

  /** Inserts a new wallet or writes the balance of an existing one. */
  void save(Wallet wallet);
}
