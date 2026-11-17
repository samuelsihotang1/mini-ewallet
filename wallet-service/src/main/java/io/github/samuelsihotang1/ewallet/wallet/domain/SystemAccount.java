package io.github.samuelsihotang1.ewallet.wallet.domain;

import java.util.UUID;

/**
 * Ledger accounts owned by the platform rather than a user. They have entries but no stored
 * balance and are never locked: every top-up touches {@link #TOP_UP_SOURCE}, and locking one row
 * for all of them would serialize every top-up in the country. Their balance, when needed, is the
 * sum of their entries.
 */
public enum SystemAccount {
  /** Where top-up money enters the ledger from outside (bank, card, minimarket). */
  TOP_UP_SOURCE("00000000-0000-0000-0000-000000000001"),

  /**
   * Holds the money of transfers between their debit and their credit or refund. Its balance is
   * zero once every transfer has finished; anything else is money stuck in a saga.
   */
  TRANSFER_SUSPENSE("00000000-0000-0000-0000-000000000002");

  private final WalletId id;

  SystemAccount(String uuid) {
    this.id = new WalletId(UUID.fromString(uuid));
  }

  /** The fixed ledger account id of this system account. */
  public WalletId id() {
    return id;
  }
}
