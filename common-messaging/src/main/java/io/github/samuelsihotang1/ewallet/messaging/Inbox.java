package io.github.samuelsihotang1.ewallet.messaging;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Remembers which events a consumer already handled, making an at-least-once consumer behave
 * exactly once in its own database.
 */
public class Inbox {

  private final JdbcClient jdbc;
  private final Clock clock;

  public Inbox(JdbcClient jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /**
   * Records that {@code consumer} handles {@code eventId} now, inside the caller's transaction.
   *
   * <p>Returns false when the event was already recorded: the caller skips its effect. Two
   * copies arriving at once cannot both win: MySQL makes the second {@code INSERT IGNORE} wait on
   * the primary key until the first transaction ends, then ignores it. If the first rolls back,
   * the second becomes the first delivery.
   *
   * @throws IllegalStateException when no transaction is active
   */
  public boolean firstDelivery(UUID eventId, String consumer) {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new IllegalStateException(
          "Inbox.firstDelivery must run inside the transaction of the effect it guards");
    }
    return jdbc.sql(
                """
                INSERT IGNORE INTO processed_event (event_id, consumer, processed_at)
                VALUES (:eventId, :consumer, :processedAt)
                """)
            .param("eventId", eventId.toString())
            .param("consumer", consumer)
            .param("processedAt", Timestamp.from(clock.instant()))
            .update()
        == 1;
  }
}
