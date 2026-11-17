package io.github.samuelsihotang1.ewallet.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import io.github.samuelsihotang1.ewallet.events.EventCodec;
import io.github.samuelsihotang1.ewallet.events.TopUpCompleted;
import io.github.samuelsihotang1.ewallet.events.WalletEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(
    properties = {
      "spring.kafka.consumer.auto-offset-reset=earliest",
      "spring.sql.init.mode=always",
      "messaging.retry.initial-interval=50ms",
      "messaging.retry.max-attempts=3"
    })
class MessagingIT {

  static final String TOPIC = "test.wallet.events.v1";
  static final String FLAKY_TOPIC = "test.flaky.v1";
  static final String POISON_TOPIC = "test.poison.v1";

  @Autowired Outbox outbox;
  @Autowired Inbox inbox;
  @Autowired TransactionTemplate transaction;
  @Autowired JdbcClient jdbc;
  @Autowired EventCodec codec;
  @Autowired Listeners listeners;

  private static TopUpCompleted topUp() {
    return new TopUpCompleted(
        UUID.randomUUID(), UUID.randomUUID(), "user-1", "10.00", "10.00", Instant.now());
  }

  @Test
  void an_event_written_in_a_committed_transaction_is_published_with_its_key() {
    var event = topUp();

    transaction.executeWithoutResult(status -> outbox.add(TOPIC, event));

    await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
        assertThat(listeners.received).anySatisfy(record -> {
          assertThat(record.key()).isEqualTo(event.partitionKey());
          assertThat(codec.decode(record.value(), WalletEvent.class)).isEqualTo(event);
        }));
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
        assertThat(publishedAt(event.eventId())).isNotNull());
  }

  @Test
  void an_event_written_in_a_rolled_back_transaction_is_never_published() {
    var event = topUp();

    transaction.executeWithoutResult(status -> {
      outbox.add(TOPIC, event);
      status.setRollbackOnly();
    });

    assertThat(
            jdbc.sql("SELECT COUNT(*) FROM outbox_event WHERE id = :id")
                .param("id", event.eventId().toString())
                .query(Long.class)
                .single())
        .isZero();
  }

  @Test
  void writing_to_the_outbox_outside_a_transaction_is_a_bug() {
    assertThatThrownBy(() -> outbox.add(TOPIC, topUp()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("transaction");
  }

  @Test
  void the_inbox_accepts_an_event_id_once() {
    var eventId = UUID.randomUUID();

    var first = transaction.execute(status -> inbox.firstDelivery(eventId, "test-consumer"));
    var again = transaction.execute(status -> inbox.firstDelivery(eventId, "test-consumer"));
    var otherConsumer = transaction.execute(status -> inbox.firstDelivery(eventId, "other"));

    assertThat(first).isTrue();
    assertThat(again).isFalse();
    assertThat(otherConsumer).isTrue();
  }

  @Test
  void a_failing_record_is_retried_then_parked_on_the_dead_letter_topic() {
    var event = topUp();

    transaction.executeWithoutResult(status -> outbox.add(FLAKY_TOPIC, event));

    await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
        assertThat(listeners.deadLetters).anySatisfy(record ->
            assertThat(record.value()).contains(event.eventId().toString())));
    assertThat(listeners.flakyAttempts.get()).isEqualTo(3);
  }

  @Test
  void an_unreadable_record_goes_to_the_dead_letter_topic_without_retries() {
    var before = listeners.poisonAttempts.get();

    listeners.sendRaw(POISON_TOPIC, "not json at all");

    await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
        assertThat(listeners.deadLetters).anySatisfy(record ->
            assertThat(record.value()).isEqualTo("not json at all")));
    assertThat(listeners.poisonAttempts.get() - before).isEqualTo(1);
  }

  private Instant publishedAt(UUID eventId) {
    return jdbc.sql("SELECT published_at FROM outbox_event WHERE id = :id")
        .param("id", eventId.toString())
        .query(Instant.class)
        .optional()
        .orElse(null);
  }

  // Spring Boot's test filter skips classes nested in a test class, so Listeners is imported.
  @SpringBootApplication
  @org.springframework.context.annotation.Import(Listeners.class)
  static class TestApplication {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysql() {
      return new MySQLContainer<>("mysql:8.4");
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
      return new KafkaContainer("apache/kafka:4.3.2");
    }

    @Bean
    org.apache.kafka.clients.admin.NewTopic testTopic() {
      return TopicBuilder.name(TOPIC).partitions(1).build();
    }

    @Bean
    org.apache.kafka.clients.admin.NewTopic flakyTopic() {
      return TopicBuilder.name(FLAKY_TOPIC).partitions(1).build();
    }

    @Bean
    org.apache.kafka.clients.admin.NewTopic poisonTopic() {
      return TopicBuilder.name(POISON_TOPIC).partitions(1).build();
    }
  }

  static class Listeners {

    final List<ConsumerRecord<String, String>> received = new CopyOnWriteArrayList<>();
    final List<ConsumerRecord<String, String>> deadLetters = new CopyOnWriteArrayList<>();
    final AtomicInteger flakyAttempts = new AtomicInteger();
    final AtomicInteger poisonAttempts = new AtomicInteger();
    private final EventCodec codec;
    private final org.springframework.kafka.core.KafkaTemplate<String, String> kafka;

    Listeners(
        EventCodec codec, org.springframework.kafka.core.KafkaTemplate<String, String> kafka) {
      this.codec = codec;
      this.kafka = kafka;
    }

    void sendRaw(String topic, String value) {
      kafka.send(topic, "raw", value);
    }

    @KafkaListener(topics = TOPIC, groupId = "messaging-it")
    void onEvent(ConsumerRecord<String, String> record) {
      received.add(record);
    }

    @KafkaListener(topics = FLAKY_TOPIC, groupId = "messaging-it")
    void onFlaky(ConsumerRecord<String, String> record) {
      flakyAttempts.incrementAndGet();
      throw new IllegalStateException("downstream is down");
    }

    @KafkaListener(topics = POISON_TOPIC, groupId = "messaging-it")
    void onPoison(ConsumerRecord<String, String> record) {
      poisonAttempts.incrementAndGet();
      codec.decode(record.value(), WalletEvent.class);
    }

    @KafkaListener(
        topics = {FLAKY_TOPIC + ".DLT", POISON_TOPIC + ".DLT"},
        groupId = "messaging-it-dlt")
    void onDeadLetter(ConsumerRecord<String, String> record) {
      deadLetters.add(record);
    }
  }
}
