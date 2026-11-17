package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import java.util.List;

/** Reads a wallet's history straight from the ledger. */
public interface MutationReader {

  /** One page of {@code walletId}'s mutations, newest first; {@code page} starts at 0. */
  List<Mutation> list(WalletId walletId, int page, int size);
}
