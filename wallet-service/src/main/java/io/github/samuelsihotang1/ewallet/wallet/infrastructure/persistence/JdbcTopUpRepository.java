package io.github.samuelsihotang1.ewallet.wallet.infrastructure.persistence;

import io.github.samuelsihotang1.ewallet.wallet.domain.JournalId;
import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.TopUp;
import io.github.samuelsihotang1.ewallet.wallet.domain.TopUpRepository;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** {@link TopUpRepository} on MySQL with plain SQL. */
@Repository
class JdbcTopUpRepository implements TopUpRepository {

  private final JdbcClient jdbc;

  JdbcTopUpRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<TopUp> findByIdempotencyKey(WalletId walletId, String idempotencyKey) {
    return jdbc.sql(
            """
            SELECT id, wallet_id, idempotency_key, amount, journal_id, balance_after, created_at
            FROM top_up WHERE wallet_id = :walletId AND idempotency_key = :key
            """)
        .param("walletId", walletId.toString())
        .param("key", idempotencyKey)
        .query(
            (rs, row) ->
                new TopUp(
                    UUID.fromString(rs.getString("id")),
                    WalletId.of(rs.getString("wallet_id")),
                    rs.getString("idempotency_key"),
                    Money.of(rs.getBigDecimal("amount"), Money.IDR),
                    new JournalId(UUID.fromString(rs.getString("journal_id"))),
                    Money.of(rs.getBigDecimal("balance_after"), Money.IDR),
                    rs.getTimestamp("created_at").toInstant()))
        .optional();
  }

  @Override
  public void save(TopUp topUp) {
    jdbc.sql(
            """
            INSERT INTO top_up
              (id, wallet_id, idempotency_key, amount, journal_id, balance_after, created_at)
            VALUES (:id, :walletId, :key, :amount, :journalId, :balanceAfter, :createdAt)
            """)
        .param("id", topUp.id().toString())
        .param("walletId", topUp.walletId().toString())
        .param("key", topUp.idempotencyKey())
        .param("amount", topUp.amount().amount())
        .param("journalId", topUp.journalId().toString())
        .param("balanceAfter", topUp.balanceAfter().amount())
        .param("createdAt", Timestamp.from(topUp.createdAt()))
        .update();
  }
}
