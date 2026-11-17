package io.github.samuelsihotang1.ewallet.wallet.domain;

/** A debit larger than the wallet's balance. */
public class InsufficientBalanceException extends DomainException {

  public InsufficientBalanceException(WalletId walletId, Money balance, Money requested) {
    super("wallet " + walletId + " has " + balance + ", cannot debit " + requested);
  }

  @Override
  public String code() {
    return "INSUFFICIENT_BALANCE";
  }
}
