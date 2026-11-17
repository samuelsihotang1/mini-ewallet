package io.github.samuelsihotang1.ewallet.transfer.domain;

/** A request the transfer rules refuse; {@link #code()} is the stable code clients see. */
public abstract class DomainException extends RuntimeException {

  protected DomainException(String message) {
    super(message);
  }

  /** A stable machine-readable code, such as {@code SAME_WALLET}. */
  public abstract String code();
}
