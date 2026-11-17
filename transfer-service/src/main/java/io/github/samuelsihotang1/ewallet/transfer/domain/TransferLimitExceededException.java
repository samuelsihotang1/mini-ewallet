package io.github.samuelsihotang1.ewallet.transfer.domain;

/** A transfer above the per-transaction limit. */
public class TransferLimitExceededException extends DomainException {

  public TransferLimitExceededException(Money amount, Money limit) {
    super("transfer of " + amount + " exceeds the limit of " + limit);
  }

  @Override
  public String code() {
    return "TRANSFER_LIMIT_EXCEEDED";
  }
}
