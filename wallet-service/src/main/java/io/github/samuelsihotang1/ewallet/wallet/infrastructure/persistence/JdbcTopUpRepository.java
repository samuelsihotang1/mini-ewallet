package io.github.samuelsihotang1.ewallet.wallet.infrastructure.persistence;

import io.github.samuelsihotang1.ewallet.wallet.domain.TopUp;
import io.github.samuelsihotang1.ewallet.wallet.domain.TopUpRepository;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** {@link TopUpRepository} on MySQL with plain SQL. */
@Repository
class JdbcTopUpRepository implements TopUpRepository {

  @Override
  public Optional<TopUp> findByIdempotencyKey(WalletId walletId, String idempotencyKey) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void save(TopUp topUp) {
    throw new UnsupportedOperationException("not implemented");
  }
}
