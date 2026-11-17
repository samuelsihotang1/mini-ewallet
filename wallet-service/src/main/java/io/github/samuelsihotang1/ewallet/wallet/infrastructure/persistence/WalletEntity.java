package io.github.samuelsihotang1.ewallet.wallet.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;

/** Table mapping of a wallet row; the domain never sees this class. */
@Entity
@Table(name = "wallet")
class WalletEntity {

  // CHAR, not Hibernate's default VARCHAR, so ddl-auto=validate matches the CHAR(36) column.
  @Id
  @JdbcTypeCode(Types.CHAR)
  @Column(length = 36)
  private String id;

  @Column(name = "owner_id", nullable = false, updatable = false)
  private String ownerId;

  @JdbcTypeCode(Types.CHAR)
  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balance;

  // Second guard next to the row lock: an update that skipped the lock fails instead of
  // silently overwriting a newer balance.
  @Version private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected WalletEntity() {}

  WalletEntity(String id, String ownerId, String currency, BigDecimal balance, Instant createdAt) {
    this.id = id;
    this.ownerId = ownerId;
    this.currency = currency;
    this.balance = balance;
    this.createdAt = createdAt;
  }

  String id() {
    return id;
  }

  String ownerId() {
    return ownerId;
  }

  String currency() {
    return currency;
  }

  BigDecimal balance() {
    return balance;
  }

  long version() {
    return version;
  }

  void changeBalance(BigDecimal newBalance) {
    this.balance = newBalance;
  }
}
