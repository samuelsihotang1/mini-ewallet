package io.github.samuelsihotang1.ewallet.transfer.infrastructure.messaging;

import io.github.samuelsihotang1.ewallet.events.Topics;
import io.github.samuelsihotang1.ewallet.events.TransferEvent;
import io.github.samuelsihotang1.ewallet.messaging.Outbox;
import io.github.samuelsihotang1.ewallet.transfer.application.TransferEvents;
import org.springframework.stereotype.Component;

/** Publishes transfer events through the transactional outbox. */
@Component
class OutboxTransferEvents implements TransferEvents {

  private final Outbox outbox;

  OutboxTransferEvents(Outbox outbox) {
    this.outbox = outbox;
  }

  @Override
  public void publish(TransferEvent event) {
    outbox.add(Topics.TRANSFER_EVENTS, event);
  }
}
