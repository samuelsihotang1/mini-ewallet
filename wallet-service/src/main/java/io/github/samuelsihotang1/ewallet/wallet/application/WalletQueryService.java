package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletRepository;
import java.util.List;

/** Read-only views of a wallet: balance and history. */
public class WalletQueryService {

  private final WalletRepository wallets;
  private final MutationReader mutations;

  public WalletQueryService(WalletRepository wallets, MutationReader mutations) {
    this.wallets = wallets;
    this.mutations = mutations;
  }

  public WalletView get(WalletId id) {
    throw new UnsupportedOperationException("not implemented");
  }

  public List<Mutation> mutations(WalletId id, int page, int size) {
    throw new UnsupportedOperationException("not implemented");
  }
}
