package io.github.samuelsihotang1.ewallet.transfer.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifies a transfer; also the Kafka key of all its events. */
public record TransferId(UUID value) {

  public TransferId {
    Objects.requireNonNull(value, "transfer id");
  }

  /** A new random id. */
  public static TransferId random() {
    return new TransferId(UUID.randomUUID());
  }

  /** Parses a UUID string. */
  public static TransferId of(String value) {
    return new TransferId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
