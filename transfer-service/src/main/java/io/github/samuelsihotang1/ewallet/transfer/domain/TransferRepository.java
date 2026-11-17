package io.github.samuelsihotang1.ewallet.transfer.domain;

import java.util.Optional;
import java.util.UUID;

/** Storage of transfers. */
public interface TransferRepository {

  /** The transfer, read without a lock. */
  Optional<Transfer> findById(TransferId id);

  /** The transfer, locked until the current transaction ends. */
  Optional<Transfer> lockById(TransferId id);

  /** The transfer the source wallet already requested with this key. */
  Optional<Transfer> findByIdempotencyKey(UUID sourceWalletId, String idempotencyKey);

  /**
   * Stores a new transfer.
   *
   * @throws org.springframework.dao.DuplicateKeyException if the source already used the key
   */
  void add(Transfer transfer);

  /**
   * Stores the new state of a transfer read at {@link Transfer#version()}.
   *
   * @throws org.springframework.dao.OptimisticLockingFailureException if it changed since
   */
  void update(Transfer transfer);
}
