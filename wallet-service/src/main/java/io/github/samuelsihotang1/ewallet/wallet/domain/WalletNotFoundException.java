package io.github.samuelsihotang1.ewallet.wallet.domain;

/** No wallet with the requested id. */
public class WalletNotFoundException extends DomainException {

  public WalletNotFoundException(WalletId id) {
    super("wallet " + id + " not found");
  }

  @Override
  public String code() {
    return "WALLET_NOT_FOUND";
  }
}
