package io.github.samuelsihotang1.ewallet.transfer.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A transfer of money from one wallet to another: the aggregate that owns the saga's state.
 *
 * <p>Each step method moves the transfer on only from the one status the step belongs to and
 * returns whether it did. A repeated or out-of-order answer from the wallet service returns
 * false and changes nothing, so the caller publishes the next command only once.
 */
public final class Transfer {

  /** The most one transfer may move. */
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

  /**
   * Checks the rules that need no other service, so a bad request fails before any remote call.
   *
   * @throws SameWalletTransferException if source and destination are one wallet
   * @throws InvalidMoneyException if the amount is not positive
   * @throws TransferLimitExceededException if the amount is above {@link #MAX_AMOUNT}
   */
  public static void requireValid(UUID sourceWalletId, UUID destinationWalletId, Money amount) {
    if (sourceWalletId.equals(destinationWalletId)) {
      throw new SameWalletTransferException(sourceWalletId);
    }
    if (!amount.isPositive()) {
      throw new InvalidMoneyException("amount must be positive, was " + amount);
    }
    if (amount.isGreaterThan(MAX_AMOUNT)) {
      throw new TransferLimitExceededException(amount, MAX_AMOUNT);
    }
  }

  /** A new PENDING transfer; see {@link #requireValid} for what it rejects. */
  public static Transfer request(
      TransferId id, Party source, Party destination, Money amount, String idempotencyKey,
      Instant now) {
    requireValid(source.walletId(), destination.walletId(), amount);
    return new Transfer(id, source, destination, amount, idempotencyKey,
        TransferStatus.PENDING, null, now, now, 0);
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

  /** PENDING to DEBITED: the source paid into suspense. */
  public boolean debited(Instant now) {
    return move(TransferStatus.PENDING, TransferStatus.DEBITED, null, now);
  }

  /** PENDING to FAILED: the source could not pay, for {@code reason}. */
  public boolean debitFailed(String reason, Instant now) {
    return move(TransferStatus.PENDING, TransferStatus.FAILED, reason, now);
  }

  /** DEBITED to COMPLETED: the destination received the money. */
  public boolean credited(Instant now) {
    return move(TransferStatus.DEBITED, TransferStatus.COMPLETED, null, now);
  }

  /** DEBITED to COMPENSATING: the destination refused, for {@code reason}; refund the source. */
  public boolean creditFailed(String reason, Instant now) {
    return move(TransferStatus.DEBITED, TransferStatus.COMPENSATING, reason, now);
  }

  /** COMPENSATING to COMPENSATED: the source has its money back. */
  public boolean refunded(Instant now) {
    return move(TransferStatus.COMPENSATING, TransferStatus.COMPENSATED, null, now);
  }

  /** Whether a retry with this transfer's key asks for the same thing. */
  public boolean sameRequestAs(UUID destinationWalletId, Money amount) {
    return destination.walletId().equals(destinationWalletId) && this.amount.equals(amount);
  }

  private boolean move(TransferStatus from, TransferStatus to, String reason, Instant now) {
    if (status != from) {
      return false;
    }
    status = to;
    if (reason != null) {
      failureReason = reason;
    }
    updatedAt = now;
    return true;
  }

  /** This transfer's id. */
  public TransferId id() {
    return id;
  }

  /** The paying wallet and its owner. */
  public Party source() {
    return source;
  }

  /** The receiving wallet and its owner. */
  public Party destination() {
    return destination;
  }

  /** The amount moved, positive and at most {@link #MAX_AMOUNT}. */
  public Money amount() {
    return amount;
  }

  /** The client's key, unique per source wallet. */
  public String idempotencyKey() {
    return idempotencyKey;
  }

  /** Where the saga is now. */
  public TransferStatus status() {
    return status;
  }

  /** Why the transfer failed, as a wallet error code; null unless it failed. */
  public String failureReason() {
    return failureReason;
  }

  /** When the transfer was requested. */
  public Instant createdAt() {
    return createdAt;
  }

  /** When the status last changed. */
  public Instant updatedAt() {
    return updatedAt;
  }

  /** The stored version this object was read at, for an optimistic check on update. */
  public long version() {
    return version;
  }
}
