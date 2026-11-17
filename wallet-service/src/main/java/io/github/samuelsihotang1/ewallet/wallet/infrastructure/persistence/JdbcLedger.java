package io.github.samuelsihotang1.ewallet.wallet.infrastructure.persistence;

import io.github.samuelsihotang1.ewallet.wallet.application.Mutation;
import io.github.samuelsihotang1.ewallet.wallet.application.MutationReader;
import io.github.samuelsihotang1.ewallet.wallet.domain.Journal;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalId;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalRepository;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalType;
import io.github.samuelsihotang1.ewallet.wallet.domain.LedgerEntry;
import io.github.samuelsihotang1.ewallet.wallet.domain.LedgerEntry.Direction;
import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The append-only ledger on MySQL, written and read with plain SQL. JPA would add dirty checking
 * and an entity cache to rows that are only ever inserted and scanned.
 */
@Repository
class JdbcLedger implements JournalRepository, MutationReader {

  private final JdbcClient jdbc;

  JdbcLedger(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean exists(JournalType type, String reference) {
    return jdbc.sql("SELECT COUNT(*) FROM journal WHERE type = :type AND reference = :reference")
            .param("type", type.name())
            .param("reference", reference)
            .query(Long.class)
            .single()
        > 0;
  }

  @Override
  public void append(Journal journal) {
    jdbc.sql(
            "INSERT INTO journal (id, type, reference, posted_at)"
                + " VALUES (:id, :type, :reference, :postedAt)")
        .param("id", journal.id().toString())
        .param("type", journal.type().name())
        .param("reference", journal.reference())
        .param("postedAt", Timestamp.from(journal.postedAt()))
        .update();
    for (LedgerEntry entry : journal.entries()) {
      jdbc.sql(
              "INSERT INTO ledger_entry"
                  + " (journal_id, account_id, direction, amount, balance_after)"
                  + " VALUES (:journalId, :accountId, :direction, :amount, :balanceAfter)")
          .param("journalId", journal.id().toString())
          .param("accountId", entry.accountId().toString())
          .param("direction", entry.direction().name())
          .param("amount", entry.amount().amount())
          .param("balanceAfter", entry.balanceAfter() == null ? null : entry.balanceAfter().amount())
          .update();
    }
  }

  @Override
  public List<Mutation> list(WalletId walletId, int page, int size) {
    return jdbc.sql(
            """
            SELECT j.id, j.type, j.reference, j.posted_at,
                   e.direction, e.amount, e.balance_after
            FROM ledger_entry e
            JOIN journal j ON j.id = e.journal_id
            WHERE e.account_id = :account
            ORDER BY e.id DESC
            LIMIT :size OFFSET :offset
            """)
        .param("account", walletId.toString())
        .param("size", size)
        .param("offset", (long) page * size)
        .query(
            (rs, row) ->
                new Mutation(
                    new JournalId(UUID.fromString(rs.getString("id"))),
                    JournalType.valueOf(rs.getString("type")),
                    Direction.valueOf(rs.getString("direction")),
                    money(rs.getBigDecimal("amount")),
                    money(rs.getBigDecimal("balance_after")),
                    rs.getString("reference"),
                    rs.getTimestamp("posted_at").toInstant()))
        .list();
  }

  private static Money money(BigDecimal amount) {
    return amount == null ? null : Money.of(amount, Money.IDR);
  }
}
