package io.github.samuelsihotang1.ewallet.wallet.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data access to wallet rows. */
interface WalletJpaRepository extends JpaRepository<WalletEntity, String> {

  /** The row, read with {@code SELECT ... FOR UPDATE}; the lock lasts until the transaction ends. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select w from WalletEntity w where w.id = :id")
  Optional<WalletEntity> findForUpdate(@Param("id") String id);

  boolean existsByOwnerId(String ownerId);
}
