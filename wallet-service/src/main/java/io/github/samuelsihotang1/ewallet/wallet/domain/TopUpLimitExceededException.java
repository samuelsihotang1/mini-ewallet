package io.github.samuelsihotang1.ewallet.wallet.domain;

/** A top-up above the per-transaction limit. */
public class TopUpLimitExceededException extends DomainException {

  public TopUpLimitExceededException(Money amount, Money limit) {
    super("top-up of " + amount + " exceeds the limit of " + limit);
  }

  @Override
  public String code() {
    return "TOP_UP_LIMIT_EXCEEDED";
  }
}
