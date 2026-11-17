package io.github.samuelsihotang1.ewallet.wallet.domain;

import java.util.Objects;
import java.util.UUID;

/** Identity of a ledger account: a customer wallet or a {@link SystemAccount}. */
public record WalletId(UUID value) {

  public WalletId {
    Objects.requireNonNull(value, "wallet id");
  }

  /** A new random id. */
  public static WalletId random() {
    return new WalletId(UUID.randomUUID());
  }

  /** Parses the canonical UUID text form. */
  public static WalletId of(String value) {
    return new WalletId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
