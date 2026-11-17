package io.github.samuelsihotang1.ewallet.wallet.domain;

/** An amount that cannot be represented exactly as rupiah with two decimals. */
public class InvalidMoneyException extends DomainException {

  public InvalidMoneyException(String message) {
    super(message);
  }

  @Override
  public String code() {
    return "INVALID_AMOUNT";
  }
}
