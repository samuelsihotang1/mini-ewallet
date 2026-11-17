package io.github.samuelsihotang1.ewallet.wallet.domain;

import java.util.Objects;
import java.util.UUID;

/** Identity of one balanced posting to the ledger. */
public record JournalId(UUID value) {

  public JournalId {
    Objects.requireNonNull(value, "journal id");
  }

  /** A new random id. */
  public static JournalId random() {
    return new JournalId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
