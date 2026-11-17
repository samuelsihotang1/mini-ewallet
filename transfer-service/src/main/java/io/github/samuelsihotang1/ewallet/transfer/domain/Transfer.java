package io.github.samuelsihotang1.ewallet.transfer.domain;

import java.time.Instant;
import java.util.UUID;

/** A transfer of money from one wallet to another, moved by a saga. */
public final class Transfer {

  public static final Money MAX_AMOUNT = Money.of("10000000.00");

  private final TransferId id;
  private final Party source;
  private final Party destination;
  private final Money amount;
  private final String idempotencyKey;
  private final Instant createdAt;
  private final long version;
  private TransferStatus status;
  private String failureReason;
  private Instant updatedAt;

  private Transfer(
      TransferId id,
      Party source,
      Party destination,
      Money amount,
      String idempotencyKey,
      TransferStatus status,
      String failureReason,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = id;
    this.source = source;
    this.destination = destination;
    this.amount = amount;
    this.idempotencyKey = idempotencyKey;
    this.status = status;
    this.failureReason = failureReason;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  public static void requireValid(UUID sourceWalletId, UUID destinationWalletId, Money amount) {
    throw new UnsupportedOperationException("not implemented");
  }

  public static Transfer request(
      TransferId id, Party source, Party destination, Money amount, String idempotencyKey,
      Instant now) {
    throw new UnsupportedOperationException("not implemented");
  }

  /** Rebuilds a transfer from storage. */
  public static Transfer restore(
      TransferId id,
      Party source,
      Party destination,
      Money amount,
      String idempotencyKey,
      TransferStatus status,
      String failureReason,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    return new Transfer(id, source, destination, amount, idempotencyKey, status, failureReason,
        createdAt, updatedAt, version);
  }

  public boolean debited(Instant now) {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean debitFailed(String reason, Instant now) {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean credited(Instant now) {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean creditFailed(String reason, Instant now) {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean refunded(Instant now) {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean sameRequestAs(UUID destinationWalletId, Money amount) {
    throw new UnsupportedOperationException("not implemented");
  }

  public TransferId id() {
    return id;
  }

  public Party source() {
    return source;
  }

  public Party destination() {
    return destination;
  }

  public Money amount() {
    return amount;
  }

  public String idempotencyKey() {
    return idempotencyKey;
  }

  public TransferStatus status() {
    return status;
  }

  public String failureReason() {
    return failureReason;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public long version() {
    return version;
  }
}
