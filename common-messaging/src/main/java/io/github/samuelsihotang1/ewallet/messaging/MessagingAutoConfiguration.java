package io.github.samuelsihotang1.ewallet.messaging;

import io.github.samuelsihotang1.ewallet.events.EventCodec;
import io.github.samuelsihotang1.ewallet.events.UnreadableEventException;
import java.time.Clock;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.support.TransactionTemplate;

/** Outbox, inbox and Kafka error handling for every service that has this module. */
@AutoConfiguration(after = {KafkaAutoConfiguration.class, TransactionAutoConfiguration.class})
@EnableConfigurationProperties(MessagingProperties.class)
@EnableScheduling
public class MessagingAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  EventCodec eventCodec() {
    return new EventCodec();
  }

  @Bean
  Outbox outbox(JdbcClient jdbc, EventCodec codec, Clock clock) {
    return new Outbox(jdbc, codec, clock);
  }

  @Bean
  Inbox inbox(JdbcClient jdbc, Clock clock) {
    return new Inbox(jdbc, clock);
  }

  @Bean
  @ConditionalOnProperty(name = "messaging.outbox.enabled", matchIfMissing = true)
  OutboxRelay outboxRelay(
      JdbcClient jdbc,
      KafkaTemplate<String, String> kafka,
      TransactionTemplate transaction,
      Clock clock,
      MessagingProperties properties) {
    return new OutboxRelay(
        jdbc,
        kafka,
        transaction,
        clock,
        properties.outbox().batchSize(),
        properties.outbox().sendTimeout());
  }

  /**
   * Retries a failing record with exponential backoff, then publishes it unchanged to
   * {@code <topic>.DLT} and moves on, so one bad record cannot block its partition. An
   * {@link UnreadableEventException} skips the retries: the same bytes will never parse.
   * Spring Boot plugs this bean into every {@code @KafkaListener} container.
   */
  @Bean
  DefaultErrorHandler kafkaErrorHandler(
      KafkaTemplate<String, String> kafka, MessagingProperties properties) {
    var recoverer =
        new DeadLetterPublishingRecoverer(
            kafka, (record, failure) -> new TopicPartition(record.topic() + ".DLT", -1));
    var backOff = new ExponentialBackOffWithMaxRetries(properties.retry().maxAttempts() - 1);
    backOff.setInitialInterval(properties.retry().initialInterval().toMillis());
    backOff.setMultiplier(2.0);
    var handler = new DefaultErrorHandler(recoverer, backOff);
    handler.addNotRetryableExceptions(UnreadableEventException.class);
    return handler;
  }
}
