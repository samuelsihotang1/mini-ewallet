package io.github.samuelsihotang1.ewallet.notification.infrastructure.deadletter;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Dead letters in MySQL, newest first. */
@Repository
public class DeadLetterStore {

  private final JdbcClient jdbc;

  DeadLetterStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  void add(String originalTopic, String key, String payload, String error, Instant receivedAt) {
    jdbc.sql("INSERT INTO dead_letter (original_topic, record_key, payload, error, received_at)"
            + " VALUES (:topic, :key, :payload, :error, :receivedAt)")
        .param("topic", originalTopic)
        .param("key", key)
        .param("payload", payload)
        .param("error", error)
        .param("receivedAt", Timestamp.from(receivedAt))
        .update();
  }

  /** A page of dead letters, newest first. */
  public List<DeadLetter> list(int page, int size) {
    return jdbc.sql("SELECT id, original_topic, record_key, payload, error, received_at"
            + " FROM dead_letter ORDER BY id DESC LIMIT :size OFFSET :offset")
        .param("size", size)
        .param("offset", (long) page * size)
        .query((row, i) -> new DeadLetter(
            row.getLong("id"),
            row.getString("original_topic"),
            row.getString("record_key"),
            row.getString("payload"),
            row.getString("error"),
            row.getTimestamp("received_at").toInstant()))
        .list();
  }
}
