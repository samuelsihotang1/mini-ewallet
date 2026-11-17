package io.github.samuelsihotang1.ewallet.wallet.infrastructure.persistence;

import io.github.samuelsihotang1.ewallet.wallet.domain.Wallet;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** {@link WalletRepository} on MySQL through JPA. */
@Repository
class JpaWalletRepository implements WalletRepository {

  @Override
  public Optional<Wallet> findById(WalletId id) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Optional<Wallet> lockById(WalletId id) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public boolean existsByOwner(String ownerId) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void save(Wallet wallet) {
    throw new UnsupportedOperationException("not implemented");
  }
}
