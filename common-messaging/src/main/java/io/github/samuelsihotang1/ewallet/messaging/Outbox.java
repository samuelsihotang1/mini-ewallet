package io.github.samuelsihotang1.ewallet.messaging;

import io.github.samuelsihotang1.ewallet.events.EventCodec;
import io.github.samuelsihotang1.ewallet.events.IntegrationEvent;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Stores events to publish in the caller's database transaction (the transactional outbox).
 *
 * <p>The event is a row committed or rolled back together with the state change it reports, so
 * the database and Kafka can never disagree about whether it happened. {@link OutboxRelay} sends
 * the rows later; a crash between send and mark sends a row twice, never zero times, which is why
 * every consumer goes through {@link Inbox}.
 */
public class Outbox {

  private final JdbcClient jdbc;
  private final EventCodec codec;
  private final Clock clock;

  public Outbox(JdbcClient jdbc, EventCodec codec, Clock clock) {
    this.jdbc = jdbc;
    this.codec = codec;
    this.clock = clock;
  }

  /**
   * Adds {@code event} for {@code topic}, keyed by {@link IntegrationEvent#partitionKey()}.
   *
   * @throws IllegalStateException when no transaction is active: the event would be published
   *     even if the caller's change rolled back
   */
  public void add(String topic, IntegrationEvent event) {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new IllegalStateException(
          "Outbox.add must run inside the transaction of the change it reports");
    }
    jdbc.sql(
            """
            INSERT INTO outbox_event
              (id, topic, partition_key, event_type, payload, created_at)
            VALUES (:id, :topic, :key, :type, :payload, :createdAt)
            """)
        .param("id", event.eventId().toString())
        .param("topic", topic)
        .param("key", event.partitionKey())
        .param("type", event.getClass().getSimpleName())
        .param("payload", codec.encode(event))
        .param("createdAt", Timestamp.from(clock.instant().truncatedTo(ChronoUnit.MICROS)))
        .update();
  }
}
