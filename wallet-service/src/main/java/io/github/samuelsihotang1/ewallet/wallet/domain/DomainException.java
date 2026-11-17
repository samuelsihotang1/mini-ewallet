package io.github.samuelsihotang1.ewallet.wallet.domain;

/** A broken business rule; {@link #code()} is the stable name clients can match on. */
public abstract class DomainException extends RuntimeException {

  protected DomainException(String message) {
    super(message);
  }

  /** A stable, machine-readable name of the broken rule, such as {@code INSUFFICIENT_BALANCE}. */
  public abstract String code();
}
