package io.github.samuelsihotang1.ewallet.notification.application;

import io.github.samuelsihotang1.ewallet.notification.domain.Notification;
import io.github.samuelsihotang1.ewallet.notification.domain.NotificationRepository;
import java.util.List;

/** Reads a user's notifications. */
public class NotificationQueryService {

  /** The largest page a client may ask for. */
  public static final int MAX_PAGE_SIZE = 100;

  public NotificationQueryService(NotificationRepository notifications) {}

  public List<Notification> list(String userId, int page, int size) {
    throw new UnsupportedOperationException("not implemented");
  }
}
