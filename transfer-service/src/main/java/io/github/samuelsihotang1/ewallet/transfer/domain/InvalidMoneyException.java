package io.github.samuelsihotang1.ewallet.transfer.domain;

/** An amount that is not exact rupiah with at most two decimals. */
public class InvalidMoneyException extends DomainException {

  public InvalidMoneyException(String message) {
    super(message);
  }

  @Override
  public String code() {
    return "INVALID_AMOUNT";
  }
}
