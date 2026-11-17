package io.github.samuelsihotang1.ewallet.wallet.domain;

/** A user's wallet: the aggregate that guards its balance against going below zero. */
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

  public static Wallet open(WalletId id, String ownerId) {
    return new Wallet(id, ownerId, Money.zero(), 0);
  }

  public static Wallet restore(WalletId id, String ownerId, Money balance, long version) {
    return new Wallet(id, ownerId, balance, version);
  }

  public void credit(Money amount) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void debit(Money amount) {
    throw new UnsupportedOperationException("not implemented");
  }

  public WalletId id() {
    return id;
  }

  public String ownerId() {
    return ownerId;
  }

  public Money balance() {
    return balance;
  }

  public long version() {
    return version;
  }
}
