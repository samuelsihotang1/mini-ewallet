package io.github.samuelsihotang1.ewallet.transfer.domain;

/** A transfer whose source and destination are one wallet. */
public class SameWalletTransferException extends DomainException {

  public SameWalletTransferException(java.util.UUID walletId) {
    super("wallet " + walletId + " cannot transfer to itself");
  }

  @Override
  public String code() {
    return "SAME_WALLET";
  }
}
