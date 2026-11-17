package io.github.samuelsihotang1.ewallet.notification.application;

import io.github.samuelsihotang1.ewallet.events.IntegrationEvent;
import io.github.samuelsihotang1.ewallet.notification.domain.NotificationRepository;

/** Turns money events into notifications for the users they concern. */
public class NotifyService {

  public NotifyService(NotificationRepository notifications) {}

  public void on(IntegrationEvent event) {
    throw new UnsupportedOperationException("not implemented");
  }
}
