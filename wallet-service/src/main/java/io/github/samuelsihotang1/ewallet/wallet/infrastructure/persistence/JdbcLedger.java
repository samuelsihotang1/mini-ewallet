package io.github.samuelsihotang1.ewallet.wallet.infrastructure.persistence;

import io.github.samuelsihotang1.ewallet.wallet.application.Mutation;
import io.github.samuelsihotang1.ewallet.wallet.application.MutationReader;
import io.github.samuelsihotang1.ewallet.wallet.domain.Journal;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalRepository;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import java.util.List;
import org.springframework.stereotype.Repository;

/** The append-only ledger on MySQL, written and read with plain SQL. */
@Repository
class JdbcLedger implements JournalRepository, MutationReader {

  @Override
  public void append(Journal journal) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public List<Mutation> list(WalletId walletId, int page, int size) {
    throw new UnsupportedOperationException("not implemented");
  }
}
