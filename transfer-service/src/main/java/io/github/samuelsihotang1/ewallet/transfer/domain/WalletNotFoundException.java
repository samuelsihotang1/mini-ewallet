package io.github.samuelsihotang1.ewallet.transfer.domain;

/** A transfer naming a wallet the wallet service does not know. */
public class WalletNotFoundException extends DomainException {

  public WalletNotFoundException(java.util.UUID walletId) {
    super("no wallet " + walletId);
  }

  @Override
  public String code() {
    return "WALLET_NOT_FOUND";
  }
}
