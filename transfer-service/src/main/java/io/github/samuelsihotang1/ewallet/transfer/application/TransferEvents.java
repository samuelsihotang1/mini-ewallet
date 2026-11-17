package io.github.samuelsihotang1.ewallet.transfer.application;

import io.github.samuelsihotang1.ewallet.events.TransferEvent;

/** Where the transfer service announces each step of a transfer. */
public interface TransferEvents {

  /** Publishes {@code event} if and only if the caller's transaction commits. */
  void publish(TransferEvent event);
}
