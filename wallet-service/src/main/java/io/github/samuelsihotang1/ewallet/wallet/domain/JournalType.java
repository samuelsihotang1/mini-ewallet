package io.github.samuelsihotang1.ewallet.wallet.domain;

/** Why money moved; shown to the user in the transaction history. */
public enum JournalType {
  TOP_UP,
  /** A transfer took money out of the source wallet. */
  TRANSFER_OUT,
  /** A transfer brought money into the destination wallet. */
  TRANSFER_IN,
  /** A transfer that could not be credited gave the money back to its source. */
  TRANSFER_REFUND
}
