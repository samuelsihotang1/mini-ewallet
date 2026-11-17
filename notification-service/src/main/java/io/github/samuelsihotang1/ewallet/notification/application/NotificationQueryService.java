package io.github.samuelsihotang1.ewallet.notification.application;

import io.github.samuelsihotang1.ewallet.notification.domain.Notification;
import io.github.samuelsihotang1.ewallet.notification.domain.NotificationRepository;
import java.util.List;

/** Reads a user's notifications. */
public class NotificationQueryService {

  /** The largest page a client may ask for. */
  public static final int MAX_PAGE_SIZE = 100;

  private final NotificationRepository notifications;

  public NotificationQueryService(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  /**
   * A page of {@code userId}'s notifications, newest first; empty for a user with none.
   *
   * @throws IllegalArgumentException if {@code page} is negative or {@code size} is not 1 to
   *     {@link #MAX_PAGE_SIZE}
   */
  public List<Notification> list(String userId, int page, int size) {
    if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException(
          "page must be >= 0 and size 1 to " + MAX_PAGE_SIZE + ", got page " + page
              + " size " + size);
    }
    return notifications.findByUser(userId, page, size);
  }
}
