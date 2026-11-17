package io.github.samuelsihotang1.ewallet.wallet.domain;

/** An idempotency key sent again with a different request body. */
public class IdempotencyKeyReusedException extends DomainException {

  public IdempotencyKeyReusedException(String idempotencyKey) {
    super("idempotency key '" + idempotencyKey + "' was already used for a different request");
  }

  @Override
  public String code() {
    return "IDEMPOTENCY_KEY_REUSED";
  }
}
