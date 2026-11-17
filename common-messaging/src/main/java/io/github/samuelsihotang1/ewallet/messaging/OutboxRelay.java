package io.github.samuelsihotang1.ewallet.messaging;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Publishes committed outbox rows to Kafka in the order they were written, then marks them sent.
 *
 * <p>Rows are claimed with {@code FOR UPDATE SKIP LOCKED}, so a second instance of the service
 * takes the next rows instead of sending the same ones. With two instances, two batches of one
 * key could interleave; consumers tolerate that because each saga step checks the transfer's
 * state before acting.
 */
public class OutboxRelay {

  /** Kafka header naming the event's type, for tools that read the topic without the codec. */
  public static final String EVENT_TYPE_HEADER = "eventType";

  private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

  private final JdbcClient jdbc;
  private final KafkaTemplate<String, String> kafka;
  private final TransactionTemplate transaction;
  private final Clock clock;
  private final int batchSize;
  private final Duration sendTimeout;

  public OutboxRelay(
      JdbcClient jdbc,
      KafkaTemplate<String, String> kafka,
      TransactionTemplate transaction,
      Clock clock,
      int batchSize,
      Duration sendTimeout) {
    this.jdbc = jdbc;
    this.kafka = kafka;
    this.transaction = transaction;
    this.clock = clock;
    this.batchSize = batchSize;
    this.sendTimeout = sendTimeout;
  }

  /** Sends every pending row, one batch per transaction, until a batch comes back short. */
  @Scheduled(fixedDelayString = "${messaging.outbox.poll-interval-ms:100}")
  public void publishPending() {
    try {
      int sent;
      do {
        sent = publishBatch();
      } while (sent == batchSize);
    } catch (RuntimeException e) {
      // The batch rolled back and stays pending; the next poll retries it.
      log.warn("outbox relay failed, will retry", e);
    }
  }

  /** Sends one batch and marks it published; returns how many rows it sent. */
  int publishBatch() {
    Integer sent = transaction.execute(status -> {
      List<PendingRow> rows =
          jdbc.sql(
                  """
                  SELECT seq, topic, partition_key, event_type, payload
                  FROM outbox_event
                  WHERE published_at IS NULL
                  ORDER BY seq
                  LIMIT :batch
                  FOR UPDATE SKIP LOCKED
                  """)
              .param("batch", batchSize)
              .query(PendingRow.class)
              .list();
      for (PendingRow row : rows) {
        send(row);
      }
      if (!rows.isEmpty()) {
        jdbc.sql("UPDATE outbox_event SET published_at = :now WHERE seq IN (:seqs)")
            .param("now", Timestamp.from(clock.instant()))
            .param("seqs", rows.stream().map(PendingRow::seq).toList())
            .update();
      }
      return rows.size();
    });
    return sent == null ? 0 : sent;
  }

  private void send(PendingRow row) {
    var record = new ProducerRecord<>(row.topic(), row.partitionKey(), row.payload());
    record.headers().add(EVENT_TYPE_HEADER, row.eventType().getBytes());
    try {
      // Waiting for the broker's ack before marking the row keeps "published" truthful.
      kafka.send(record).get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted while publishing outbox row " + row.seq(), e);
    } catch (ExecutionException | TimeoutException e) {
      throw new IllegalStateException("could not publish outbox row " + row.seq(), e);
    }
  }

  record PendingRow(long seq, String topic, String partitionKey, String eventType, String payload) {}
}
