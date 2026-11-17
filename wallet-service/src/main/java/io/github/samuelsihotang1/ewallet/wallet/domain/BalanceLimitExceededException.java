package io.github.samuelsihotang1.ewallet.wallet.domain;

/** A credit that would lift a wallet's balance above {@link Wallet#MAX_BALANCE}. */
public class BalanceLimitExceededException extends DomainException {

  public BalanceLimitExceededException(WalletId walletId, Money balance, Money amount) {
    super(
        "wallet " + walletId + " has " + balance + ", crediting " + amount
            + " would exceed the limit of " + Wallet.MAX_BALANCE);
  }

  @Override
  public String code() {
    return "BALANCE_LIMIT_EXCEEDED";
  }
}
