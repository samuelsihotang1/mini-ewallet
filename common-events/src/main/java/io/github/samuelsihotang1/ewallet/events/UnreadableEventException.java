package io.github.samuelsihotang1.ewallet.events;

/** A Kafka payload that is not a known event; retrying it can never succeed. */
public class UnreadableEventException extends RuntimeException {

  public UnreadableEventException(String message, Throwable cause) {
    super(message, cause);
  }
}
