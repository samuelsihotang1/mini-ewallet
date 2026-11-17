package io.github.samuelsihotang1.ewallet.events;

import java.time.Instant;
import java.util.UUID;

/**
 * A fact one service publishes for others. Fields may be added within a topic version; none is
 * renamed or removed.
 */
public interface IntegrationEvent {

  /** Unique per event; consumers store it to ignore a redelivered copy. */
  UUID eventId();

  /** When the fact happened, in UTC. */
  Instant occurredAt();

  /**
   * The Kafka record key. Events of one transfer share the transfer id, so they land on one
   * partition and are consumed in the order they were written.
   */
  String partitionKey();
}
