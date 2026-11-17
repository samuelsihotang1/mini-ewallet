package io.github.samuelsihotang1.ewallet.notification.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A message for one user about something that happened to their money.
 *
 * @param eventId the event that caused it; with {@code userId}, unique, so a redelivered event
 *     cannot notify a user twice
 * @param createdAt when the underlying fact happened, not when it was stored
 */
public record Notification(
    UUID id,
    String userId,
    NotificationType type,
    String message,
    UUID eventId,
    Instant createdAt) {

  public Notification {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("a notification needs a recipient");
    }
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("a notification needs a message");
    }
  }
}
