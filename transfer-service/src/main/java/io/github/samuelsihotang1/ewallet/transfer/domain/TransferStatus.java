package io.github.samuelsihotang1.ewallet.transfer.domain;

/** Where a transfer is in its saga. */
public enum TransferStatus {
  PENDING,
  DEBITED,
  COMPLETED,
  FAILED,
  COMPENSATING,
  COMPENSATED;

  public boolean isFinal() {
    throw new UnsupportedOperationException("not implemented");
  }
}
