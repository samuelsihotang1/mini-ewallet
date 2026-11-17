package io.github.samuelsihotang1.ewallet.transfer.domain;

/** An idempotency key sent again with a different destination or amount. */
public class IdempotencyKeyReusedException extends DomainException {

  public IdempotencyKeyReusedException(String key) {
    super("idempotency key " + key + " was already used for a different transfer");
  }

  @Override
  public String code() {
    return "IDEMPOTENCY_KEY_REUSED";
  }
}
