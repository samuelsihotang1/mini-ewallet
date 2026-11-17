package io.github.samuelsihotang1.ewallet.messaging;

import io.github.samuelsihotang1.ewallet.events.IntegrationEvent;

/** Stores events to publish in the caller's database transaction. */
public class Outbox {

  public void add(String topic, IntegrationEvent event) {
    throw new UnsupportedOperationException("not implemented");
  }
}
