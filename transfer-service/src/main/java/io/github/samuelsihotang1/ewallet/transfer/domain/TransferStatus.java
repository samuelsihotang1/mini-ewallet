package io.github.samuelsihotang1.ewallet.transfer.domain;

/**
 * Where a transfer is in its saga.
 *
 * <pre>
 * PENDING --debited--> DEBITED --credited--> COMPLETED
 *    |                    |
 *    +--debit failed--> FAILED
 *                         +--credit failed--> COMPENSATING --refunded--> COMPENSATED
 * </pre>
 */
public enum TransferStatus {
  /** Accepted; the wallet service has been asked to debit the source. */
  PENDING,
  /** The money left the source and sits in suspense; the destination is being credited. */
  DEBITED,
  /** The money reached the destination. */
  COMPLETED,
  /** The source could not be debited; no money moved. */
  FAILED,
  /** The destination could not be credited; the source is being refunded. */
  COMPENSATING,
  /** The source got its money back. */
  COMPENSATED;

  /** Whether the saga has ended; a final status never changes again. */
  public boolean isFinal() {
    return this == COMPLETED || this == FAILED || this == COMPENSATED;
  }
}
