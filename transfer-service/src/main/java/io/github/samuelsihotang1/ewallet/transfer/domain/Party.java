package io.github.samuelsihotang1.ewallet.transfer.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * One side of a transfer: a wallet and the user who owned it when the transfer was made. The
 * owner is copied here so the events can name both users without asking the wallet service again.
 */
public record Party(UUID walletId, String ownerId) {

  public Party {
    Objects.requireNonNull(walletId, "wallet id");
    Objects.requireNonNull(ownerId, "owner id");
  }
}
