package io.github.samuelsihotang1.ewallet.transfer.infrastructure.persistence;

import io.github.samuelsihotang1.ewallet.transfer.domain.Money;
import io.github.samuelsihotang1.ewallet.transfer.domain.Party;
import io.github.samuelsihotang1.ewallet.transfer.domain.Transfer;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferId;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferRepository;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Transfers in MySQL, one row each, with an optimistic version check on every update. */
@Repository
class JdbcTransferRepository implements TransferRepository {

  private static final String COLUMNS =
      "id, source_wallet_id, source_owner_id, destination_wallet_id, destination_owner_id,"
          + " amount, idempotency_key, status, failure_reason, created_at, updated_at, version";

  private final JdbcClient jdbc;

  JdbcTransferRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<Transfer> findById(TransferId id) {
    return jdbc.sql("SELECT " + COLUMNS + " FROM transfer WHERE id = :id")
        .param("id", id.toString())
        .query(JdbcTransferRepository::map)
        .optional();
  }

  @Override
  public Optional<Transfer> lockById(TransferId id) {
    return jdbc.sql("SELECT " + COLUMNS + " FROM transfer WHERE id = :id FOR UPDATE")
        .param("id", id.toString())
        .query(JdbcTransferRepository::map)
        .optional();
  }

  @Override
  public Optional<Transfer> findByIdempotencyKey(UUID sourceWalletId, String idempotencyKey) {
    return jdbc.sql("SELECT " + COLUMNS + " FROM transfer"
            + " WHERE source_wallet_id = :source AND idempotency_key = :key")
        .param("source", sourceWalletId.toString())
        .param("key", idempotencyKey)
        .query(JdbcTransferRepository::map)
        .optional();
  }

  /**
   * A plain INSERT. Concurrent copies of one request race on UNIQUE(source, key): the first
   * commits, the others get {@link org.springframework.dao.DuplicateKeyException}. No read or
   * update of the missing row comes first: under REPEATABLE READ that takes a gap lock, and
   * concurrent inserts into the same gap deadlock (see ConcurrentRequestIT).
   */
  @Override
  public void add(Transfer transfer) {
    jdbc.sql("INSERT INTO transfer (" + COLUMNS + ") VALUES (:id, :source, :sourceOwner,"
            + " :destination, :destinationOwner, :amount, :key, :status, :reason, :createdAt,"
            + " :updatedAt, 0)")
        .param("id", transfer.id().toString())
        .param("source", transfer.source().walletId().toString())
        .param("sourceOwner", transfer.source().ownerId())
        .param("destination", transfer.destination().walletId().toString())
        .param("destinationOwner", transfer.destination().ownerId())
        .param("amount", transfer.amount().amount())
        .param("key", transfer.idempotencyKey())
        .param("status", transfer.status().name())
        .param("reason", transfer.failureReason())
        .param("createdAt", Timestamp.from(transfer.createdAt()))
        .param("updatedAt", Timestamp.from(transfer.updatedAt()))
        .update();
  }

  /**
   * Writes the new state only if the row is still at the version it was read at, so a write that
   * skipped the row lock cannot silently overwrite another.
   */
  @Override
  public void update(Transfer transfer) {
    int updated = jdbc.sql("UPDATE transfer SET status = :status, failure_reason = :reason,"
            + " updated_at = :updatedAt, version = version + 1"
            + " WHERE id = :id AND version = :version")
        .param("status", transfer.status().name())
        .param("reason", transfer.failureReason())
        .param("updatedAt", Timestamp.from(transfer.updatedAt()))
        .param("id", transfer.id().toString())
        .param("version", transfer.version())
        .update();
    if (updated != 1) {
      throw new OptimisticLockingFailureException(
          "transfer " + transfer.id() + " is missing or changed since version "
              + transfer.version());
    }
  }

  private static Transfer map(ResultSet row, int rowNum) throws SQLException {
    return Transfer.restore(
        TransferId.of(row.getString("id")),
        new Party(UUID.fromString(row.getString("source_wallet_id")),
            row.getString("source_owner_id")),
        new Party(UUID.fromString(row.getString("destination_wallet_id")),
            row.getString("destination_owner_id")),
        Money.of(row.getBigDecimal("amount").toPlainString()),
        row.getString("idempotency_key"),
        TransferStatus.valueOf(row.getString("status")),
        row.getString("failure_reason"),
        row.getTimestamp("created_at").toInstant(),
        row.getTimestamp("updated_at").toInstant(),
        row.getLong("version"));
  }
}
