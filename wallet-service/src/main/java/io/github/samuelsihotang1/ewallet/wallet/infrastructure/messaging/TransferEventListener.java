package io.github.samuelsihotang1.ewallet.wallet.infrastructure.messaging;

import io.github.samuelsihotang1.ewallet.events.EventCodec;
import io.github.samuelsihotang1.ewallet.events.Topics;
import io.github.samuelsihotang1.ewallet.events.TransferCompensationRequested;
import io.github.samuelsihotang1.ewallet.events.TransferDebited;
import io.github.samuelsihotang1.ewallet.events.TransferEvent;
import io.github.samuelsihotang1.ewallet.events.TransferRequested;
import io.github.samuelsihotang1.ewallet.messaging.Inbox;
import io.github.samuelsihotang1.ewallet.wallet.application.TransferStepService;
import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Turns the transfer service's commands into wallet steps. The inbox row, the money movement and
 * the answering outbox event commit in one transaction, so a redelivered event changes nothing.
 */
@Component
class TransferEventListener {

  static final String CONSUMER = "wallet-service";

  private final EventCodec codec;
  private final Inbox inbox;
  private final TransferStepService steps;
  private final TransactionTemplate transaction;

  TransferEventListener(
      EventCodec codec,
      Inbox inbox,
      TransferStepService steps,
      PlatformTransactionManager transactionManager) {
    this.codec = codec;
    this.inbox = inbox;
    this.steps = steps;
    this.transaction = new TransactionTemplate(transactionManager);
    // The steps join this transaction, and a joined transaction keeps the outer isolation.
    this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
  }

  @KafkaListener(topics = Topics.TRANSFER_EVENTS, groupId = CONSUMER)
  void on(String payload) {
    var event = codec.decode(payload, TransferEvent.class);
    transaction.executeWithoutResult(status -> {
      if (inbox.firstDelivery(event.eventId(), CONSUMER)) {
        handle(event);
      }
    });
  }

  private void handle(TransferEvent event) {
    switch (event) {
      case TransferRequested e ->
          steps.debit(e.transferId(), new WalletId(e.sourceWalletId()), Money.of(e.amount()));
      case TransferDebited e ->
          steps.credit(e.transferId(), new WalletId(e.destinationWalletId()), Money.of(e.amount()));
      case TransferCompensationRequested e ->
          steps.refund(e.transferId(), new WalletId(e.sourceWalletId()), Money.of(e.amount()));
      default -> {
        // TransferCompleted and TransferFailed are for notifications; the wallet has no step.
      }
    }
  }
}
