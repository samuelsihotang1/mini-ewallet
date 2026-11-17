package io.github.samuelsihotang1.ewallet.transfer.domain;

/** No transfer with the requested id. */
public class TransferNotFoundException extends DomainException {

  public TransferNotFoundException(TransferId id) {
    super("no transfer " + id);
  }

  @Override
  public String code() {
    return "TRANSFER_NOT_FOUND";
  }
}
