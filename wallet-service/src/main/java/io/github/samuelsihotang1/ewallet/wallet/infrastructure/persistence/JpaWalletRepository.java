package io.github.samuelsihotang1.ewallet.wallet.infrastructure.persistence;

import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.Wallet;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletRepository;
import java.time.Clock;
import java.util.Currency;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** {@link WalletRepository} on MySQL through JPA, mapping rows to and from the aggregate. */
@Repository
class JpaWalletRepository implements WalletRepository {

  private final WalletJpaRepository rows;
  private final Clock clock;

  JpaWalletRepository(WalletJpaRepository rows, Clock clock) {
    this.rows = rows;
    this.clock = clock;
  }

  @Override
  public Optional<Wallet> findById(WalletId id) {
    return rows.findById(id.toString()).map(JpaWalletRepository::toDomain);
  }

  @Override
  public Optional<Wallet> lockById(WalletId id) {
    return rows.findForUpdate(id.toString()).map(JpaWalletRepository::toDomain);
  }

  @Override
  public boolean existsByOwner(String ownerId) {
    return rows.existsByOwnerId(ownerId);
  }

  /**
   * Inserts the wallet or updates its balance. Flushes at once, so a broken unique or check
   * constraint surfaces here as a {@code DataIntegrityViolationException} instead of at commit.
   */
  @Override
  public void save(Wallet wallet) {
    var row =
        rows.findById(wallet.id().toString())
            .orElseGet(
                () ->
                    new WalletEntity(
                        wallet.id().toString(),
                        wallet.ownerId(),
                        wallet.balance().currency().getCurrencyCode(),
                        wallet.balance().amount(),
                        clock.instant()));
    row.changeBalance(wallet.balance().amount());
    rows.saveAndFlush(row);
  }

  private static Wallet toDomain(WalletEntity row) {
    return Wallet.restore(
        WalletId.of(row.id()),
        row.ownerId(),
        Money.of(row.balance(), Currency.getInstance(row.currency())),
        row.version());
  }
}
