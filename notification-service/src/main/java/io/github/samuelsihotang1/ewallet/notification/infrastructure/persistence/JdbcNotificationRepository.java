package io.github.samuelsihotang1.ewallet.notification.infrastructure.persistence;

import io.github.samuelsihotang1.ewallet.notification.domain.Notification;
import io.github.samuelsihotang1.ewallet.notification.domain.NotificationRepository;
import io.github.samuelsihotang1.ewallet.notification.domain.NotificationType;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Notifications in MySQL. */
@Repository
class JdbcNotificationRepository implements NotificationRepository {

  private final JdbcClient jdbc;

  JdbcNotificationRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void saveAll(List<Notification> notifications) {
    for (var n : notifications) {
      jdbc.sql("INSERT INTO notification (id, user_id, type, message, event_id, created_at)"
              + " VALUES (:id, :userId, :type, :message, :eventId, :createdAt)")
          .param("id", n.id().toString())
          .param("userId", n.userId())
          .param("type", n.type().name())
          .param("message", n.message())
          .param("eventId", n.eventId().toString())
          .param("createdAt", Timestamp.from(n.createdAt()))
          .update();
    }
  }

  @Override
  public List<Notification> findByUser(String userId, int page, int size) {
    return jdbc.sql("SELECT id, user_id, type, message, event_id, created_at FROM notification"
            + " WHERE user_id = :userId ORDER BY created_at DESC, id LIMIT :size OFFSET :offset")
        .param("userId", userId)
        .param("size", size)
        .param("offset", (long) page * size)
        .query((row, i) -> new Notification(
            UUID.fromString(row.getString("id")),
            row.getString("user_id"),
            NotificationType.valueOf(row.getString("type")),
            row.getString("message"),
            UUID.fromString(row.getString("event_id")),
            row.getTimestamp("created_at").toInstant()))
        .list();
  }
}
