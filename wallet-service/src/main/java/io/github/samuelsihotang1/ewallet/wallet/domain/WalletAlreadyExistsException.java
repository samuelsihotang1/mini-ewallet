package io.github.samuelsihotang1.ewallet.wallet.domain;

/** The user already has a wallet; one wallet per user. */
public class WalletAlreadyExistsException extends DomainException {

  public WalletAlreadyExistsException(String ownerId) {
    super("user " + ownerId + " already has a wallet");
  }

  @Override
  public String code() {
    return "WALLET_ALREADY_EXISTS";
  }
}
