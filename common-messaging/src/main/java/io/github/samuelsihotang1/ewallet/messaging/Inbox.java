package io.github.samuelsihotang1.ewallet.messaging;

import java.util.UUID;

/** Remembers which events a consumer already handled. */
public class Inbox {

  public boolean firstDelivery(UUID eventId, String consumer) {
    throw new UnsupportedOperationException("not implemented");
  }
}
