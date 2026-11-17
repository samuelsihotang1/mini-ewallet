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

  /** Inserts a new transfer or updates a stored one. */
  void save(Transfer transfer);
}
