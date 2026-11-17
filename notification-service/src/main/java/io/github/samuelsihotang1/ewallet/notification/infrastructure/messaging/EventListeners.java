package io.github.samuelsihotang1.ewallet.notification.infrastructure.messaging;

import io.github.samuelsihotang1.ewallet.events.EventCodec;
import io.github.samuelsihotang1.ewallet.events.IntegrationEvent;
import io.github.samuelsihotang1.ewallet.events.Topics;
import io.github.samuelsihotang1.ewallet.events.TransferEvent;
import io.github.samuelsihotang1.ewallet.events.WalletEvent;
import io.github.samuelsihotang1.ewallet.messaging.Inbox;
import io.github.samuelsihotang1.ewallet.notification.application.NotifyService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Consumes both services' topics. The inbox row and the notifications commit together, so a
 * redelivered event notifies nobody twice. A failure is retried with backoff, then parked on
 * {@code <topic>.notification-service.DLT} (see {@code messaging.retry} in application.yml).
 */
@Component
class EventListeners {

  static final String CONSUMER = "notification-service";

  private final EventCodec codec;
  private final Inbox inbox;
  private final NotifyService notify;
  private final TransactionTemplate transaction;

  EventListeners(
      EventCodec codec, Inbox inbox, NotifyService notify, TransactionTemplate transaction) {
    this.codec = codec;
    this.inbox = inbox;
    this.notify = notify;
    this.transaction = transaction;
  }

  @KafkaListener(topics = Topics.TRANSFER_EVENTS, groupId = CONSUMER)
  void onTransferEvent(String payload) {
    handle(codec.decode(payload, TransferEvent.class));
  }

  @KafkaListener(topics = Topics.WALLET_EVENTS, groupId = CONSUMER)
  void onWalletEvent(String payload) {
    handle(codec.decode(payload, WalletEvent.class));
  }

  private void handle(IntegrationEvent event) {
    transaction.executeWithoutResult(status -> {
      if (inbox.firstDelivery(event.eventId(), CONSUMER)) {
        notify.on(event);
      }
    });
  }
}
