package io.github.samuelsihotang1.ewallet.notification.application;

import io.github.samuelsihotang1.ewallet.events.IntegrationEvent;
import io.github.samuelsihotang1.ewallet.events.TopUpCompleted;
import io.github.samuelsihotang1.ewallet.events.TransferCompleted;
import io.github.samuelsihotang1.ewallet.events.TransferFailed;
import io.github.samuelsihotang1.ewallet.notification.domain.Notification;
import io.github.samuelsihotang1.ewallet.notification.domain.NotificationRepository;
import io.github.samuelsihotang1.ewallet.notification.domain.NotificationType;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns money events into notifications for the users they concern. The events carry the users'
 * ids, so this service never calls another one: it keeps working while they are down.
 */
public class NotifyService {

  private final NotificationRepository notifications;

  public NotifyService(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  /** Stores the notifications {@code event} causes; most saga events cause none. */
  @Transactional
  public void on(IntegrationEvent event) {
    var created = switch (event) {
      case TransferCompleted e -> List.of(
          notify(e, e.sourceUserId(), NotificationType.TRANSFER_SENT,
              "You sent %s IDR to %s.".formatted(e.amount(), e.destinationUserId())),
          notify(e, e.destinationUserId(), NotificationType.TRANSFER_RECEIVED,
              "You received %s IDR from %s.".formatted(e.amount(), e.sourceUserId())));
      case TransferFailed e -> List.of(
          notify(e, e.sourceUserId(), NotificationType.TRANSFER_FAILED,
              "Your transfer of %s IDR to %s failed: %s.%s".formatted(
                  e.amount(), e.destinationUserId(), e.reason(),
                  e.compensated() ? " The money is back in your wallet." : "")));
      case TopUpCompleted e -> List.of(
          notify(e, e.userId(), NotificationType.TOP_UP,
              "Top-up of %s IDR received. Balance: %s IDR.".formatted(
                  e.amount(), e.balanceAfter())));
      default -> List.<Notification>of();
    };
    if (!created.isEmpty()) {
      notifications.saveAll(created);
    }
  }

  private static Notification notify(
      IntegrationEvent cause, String userId, NotificationType type, String message) {
    return new Notification(
        UUID.randomUUID(), userId, type, message, cause.eventId(), cause.occurredAt());
  }
}
