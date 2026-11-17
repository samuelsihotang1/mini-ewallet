package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletNotFoundException;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletRepository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/** Read-only views of a wallet: balance and history. */
public class WalletQueryService {

  /** The largest history page a client may ask for. */
  public static final int MAX_PAGE_SIZE = 100;

  private final WalletRepository wallets;
  private final MutationReader mutations;

  public WalletQueryService(WalletRepository wallets, MutationReader mutations) {
    this.wallets = wallets;
    this.mutations = mutations;
  }

  /**
   * The wallet and its current balance.
   *
   * @throws WalletNotFoundException if it does not exist
   */
  @Transactional(readOnly = true)
  public WalletView get(WalletId id) {
    return wallets.findById(id).map(WalletView::of).orElseThrow(() -> new WalletNotFoundException(id));
  }

  /**
   * One page of the wallet's history, newest first.
   *
   * @throws WalletNotFoundException if it does not exist
   * @throws IllegalArgumentException if {@code page} is negative or {@code size} is outside 1 to
   *     {@link #MAX_PAGE_SIZE}
   */
  @Transactional(readOnly = true)
  public List<Mutation> mutations(WalletId id, int page, int size) {
    if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException(
          "page must be >= 0 and size between 1 and " + MAX_PAGE_SIZE);
    }
    if (wallets.findById(id).isEmpty()) {
      throw new WalletNotFoundException(id);
    }
    return mutations.list(id, page, size);
  }
}
