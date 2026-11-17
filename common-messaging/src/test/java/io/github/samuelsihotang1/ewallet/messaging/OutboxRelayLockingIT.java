package io.github.samuelsihotang1.ewallet.messaging;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.github.samuelsihotang1.ewallet.events.TopUpCompleted;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The relay keeps its claim open while it waits for the broker. That wait must not block the
 * services' own transactions, which write new outbox rows while holding a wallet or transfer
 * lock. The scheduled relay is off, so only the relay under test claims rows.
 */
@SpringBootTest(
    classes = MessagingIT.TestApplication.class,
    properties = {"spring.sql.init.mode=always", "messaging.outbox.enabled=false"})
class OutboxRelayLockingIT {

  @Autowired Outbox outbox;
  @Autowired TransactionTemplate transaction;
  @Autowired JdbcClient jdbc;

  private void addEvent() {
    transaction.executeWithoutResult(status -> outbox.add("test.locking.v1", new TopUpCompleted(
        UUID.randomUUID(), UUID.randomUUID(), "user-1", "10.00", "10.00", Instant.now())));
  }

  @Test
  @SuppressWarnings("unchecked")
  void a_new_outbox_row_commits_while_the_relay_waits_for_the_broker() throws Exception {
    addEvent();
    var sending = new CountDownLatch(1);
    var ack = new CompletableFuture<SendResult<String, String>>();
    KafkaTemplate<String, String> broker = mock(KafkaTemplate.class);
    given(broker.send(any(ProducerRecord.class))).willAnswer(call -> {
      sending.countDown();
      return ack;
    });
    var relay = new OutboxRelay(
        jdbc, broker, transaction, Clock.systemUTC(), 10, Duration.ofSeconds(30));

    try (var pool = Executors.newFixedThreadPool(2)) {
      var relaying = pool.submit(relay::publishBatch);
      assertThat(sending.await(10, SECONDS)).as("relay claimed the row").isTrue();

      var writing = pool.submit(this::addEvent);
      try {
        assertThatCode(() -> writing.get(3, SECONDS)).doesNotThrowAnyException();
      } finally {
        ack.complete(null);
      }
      assertThat(relaying.get(10, SECONDS)).isEqualTo(1);
    }
  }
}
