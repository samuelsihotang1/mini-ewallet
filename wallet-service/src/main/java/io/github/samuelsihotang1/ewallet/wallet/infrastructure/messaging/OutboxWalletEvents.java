package io.github.samuelsihotang1.ewallet.wallet.infrastructure.messaging;

import io.github.samuelsihotang1.ewallet.events.WalletEvent;
import io.github.samuelsihotang1.ewallet.wallet.application.WalletEvents;
import org.springframework.stereotype.Component;

/** Publishes wallet events through the transactional outbox. */
@Component
class OutboxWalletEvents implements WalletEvents {

  @Override
  public void publish(WalletEvent event) {
    throw new UnsupportedOperationException("not implemented");
  }
}
