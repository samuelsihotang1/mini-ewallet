package io.github.samuelsihotang1.ewallet.transfer.infrastructure.messaging;

import io.github.samuelsihotang1.ewallet.events.CreditFailed;
import io.github.samuelsihotang1.ewallet.events.DebitFailed;
import io.github.samuelsihotang1.ewallet.events.EventCodec;
import io.github.samuelsihotang1.ewallet.events.Topics;
import io.github.samuelsihotang1.ewallet.events.TopUpCompleted;
import io.github.samuelsihotang1.ewallet.events.WalletCredited;
import io.github.samuelsihotang1.ewallet.events.WalletDebited;
import io.github.samuelsihotang1.ewallet.events.WalletEvent;
import io.github.samuelsihotang1.ewallet.events.WalletRefunded;
import io.github.samuelsihotang1.ewallet.messaging.Inbox;
import io.github.samuelsihotang1.ewallet.transfer.application.TransferSagaService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Feeds the wallet service's answers into the saga. The inbox row, the state change and the next
 * outbox event commit together, so a redelivered answer changes nothing.
 */
@Component
class WalletEventListener {

  static final String CONSUMER = "transfer-service";

  private final EventCodec codec;
  private final Inbox inbox;
  private final TransferSagaService saga;
  private final TransactionTemplate transaction;

  WalletEventListener(
      EventCodec codec, Inbox inbox, TransferSagaService saga, TransactionTemplate transaction) {
    this.codec = codec;
    this.inbox = inbox;
    this.saga = saga;
    this.transaction = transaction;
  }

  @KafkaListener(topics = Topics.WALLET_EVENTS, groupId = CONSUMER)
  void on(String payload) {
    var event = codec.decode(payload, WalletEvent.class);
    if (event instanceof TopUpCompleted) {
      return;
    }
    transaction.executeWithoutResult(status -> {
      if (inbox.firstDelivery(event.eventId(), CONSUMER)) {
        handle(event);
      }
    });
  }

  private void handle(WalletEvent event) {
    switch (event) {
      case WalletDebited e -> saga.onDebited(e.transferId());
      case DebitFailed e -> saga.onDebitFailed(e.transferId(), e.reason());
      case WalletCredited e -> saga.onCredited(e.transferId());
      case CreditFailed e -> saga.onCreditFailed(e.transferId(), e.reason());
      case WalletRefunded e -> saga.onRefunded(e.transferId());
      case TopUpCompleted e -> {
        // Filtered out above; top-ups are not part of any transfer.
      }
    }
  }
}
