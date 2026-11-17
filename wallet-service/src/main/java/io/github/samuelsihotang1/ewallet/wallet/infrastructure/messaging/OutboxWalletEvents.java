package io.github.samuelsihotang1.ewallet.wallet.infrastructure.messaging;

import io.github.samuelsihotang1.ewallet.events.Topics;
import io.github.samuelsihotang1.ewallet.events.WalletEvent;
import io.github.samuelsihotang1.ewallet.messaging.Outbox;
import io.github.samuelsihotang1.ewallet.wallet.application.WalletEvents;
import org.springframework.stereotype.Component;

/** Publishes wallet events through the transactional outbox. */
@Component
class OutboxWalletEvents implements WalletEvents {

  private final Outbox outbox;

  OutboxWalletEvents(Outbox outbox) {
    this.outbox = outbox;
  }

  @Override
  public void publish(WalletEvent event) {
    outbox.add(Topics.WALLET_EVENTS, event);
  }
}
