package io.github.samuelsihotang1.ewallet.notification.application;

import io.github.samuelsihotang1.ewallet.notification.domain.Notification;
import io.github.samuelsihotang1.ewallet.notification.domain.NotificationRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A fake of the notification store, kept in memory. */
class InMemoryNotifications implements NotificationRepository {

  final List<Notification> saved = new ArrayList<>();

  @Override
  public void saveAll(List<Notification> notifications) {
    saved.addAll(notifications);
  }

  @Override
  public List<Notification> findByUser(String userId, int page, int size) {
    return saved.stream()
        .filter(n -> n.userId().equals(userId))
        .sorted(Comparator.comparing(Notification::createdAt).reversed())
        .skip((long) page * size)
        .limit(size)
        .toList();
  }
}
