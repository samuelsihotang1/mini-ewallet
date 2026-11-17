package io.github.samuelsihotang1.ewallet.wallet.domain;

/**
 * A user's wallet: the aggregate that guards its balance against going below zero.
 *
 * <p>The balance is a cached projection of the wallet's ledger entries, kept in the same
 * transaction as the entries that change it. Callers move money only through a {@link Journal},
 * which records the entry for every change made here.
 */
public final class Wallet {

  private final WalletId id;
  private final String ownerId;
  private Money balance;
  private final long version;

  private Wallet(WalletId id, String ownerId, Money balance, long version) {
    this.id = id;
    this.ownerId = ownerId;
    this.balance = balance;
    this.version = version;
  }

  /** Opens an empty wallet for {@code ownerId}. */
  public static Wallet open(WalletId id, String ownerId) {
    if (ownerId == null || ownerId.isBlank()) {
      throw new IllegalArgumentException("a wallet needs an owner");
    }
    return new Wallet(id, ownerId, Money.zero(), 0);
  }

  /** Rebuilds a wallet from storage, with the version it was read at. */
  public static Wallet restore(WalletId id, String ownerId, Money balance, long version) {
    return new Wallet(id, ownerId, balance, version);
  }

  /** Adds a positive amount to the balance. */
  void credit(Money amount) {
    requirePositive(amount);
    balance = balance.plus(amount);
  }

  /**
   * Takes a positive amount out of the balance.
   *
   * @throws InsufficientBalanceException if the amount exceeds the balance; nothing changes
   */
  void debit(Money amount) {
    requirePositive(amount);
    if (amount.isGreaterThan(balance)) {
      throw new InsufficientBalanceException(id, balance, amount);
    }
    balance = balance.minus(amount);
  }

  private static void requirePositive(Money amount) {
    if (!amount.isPositive()) {
      throw new InvalidMoneyException("amount must be positive, was " + amount);
    }
  }

  /** This wallet's ledger account id. */
  public WalletId id() {
    return id;
  }

  /** The user who owns this wallet. */
  public String ownerId() {
    return ownerId;
  }

  /** The current balance, never negative. */
  public Money balance() {
    return balance;
  }

  /** The version this wallet was read at, for optimistic checks in storage. */
  public long version() {
    return version;
  }
}
