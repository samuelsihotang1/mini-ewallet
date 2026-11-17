package io.github.samuelsihotang1.ewallet.notification.domain;

import java.util.List;

/** Storage of notifications. */
public interface NotificationRepository {

  /** Stores the notifications of one event together. */
  void saveAll(List<Notification> notifications);

  /** A page of {@code userId}'s notifications, newest first. */
  List<Notification> findByUser(String userId, int page, int size);
}
