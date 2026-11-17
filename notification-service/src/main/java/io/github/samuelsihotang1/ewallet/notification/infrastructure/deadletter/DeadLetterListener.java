package io.github.samuelsihotang1.ewallet.notification.infrastructure.deadletter;

import io.github.samuelsihotang1.ewallet.events.Topics;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

/**
 * Copies this service's dead letters into the database, where the API lists them. Spring Kafka's
 * dead-letter publisher stores the original topic and the exception in record headers.
 */
@Component
class DeadLetterListener {

  /** Must equal {@code messaging.retry.dead-letter-suffix} in application.yml. */
  static final String SUFFIX = ".notification-service.DLT";

  static final String TRANSFER_DLT = Topics.TRANSFER_EVENTS + SUFFIX;
  static final String WALLET_DLT = Topics.WALLET_EVENTS + SUFFIX;

  private final DeadLetterStore store;
  private final Clock clock;

  DeadLetterListener(DeadLetterStore store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  @KafkaListener(topics = {TRANSFER_DLT, WALLET_DLT}, groupId = "notification-service-dlt")
  void on(ConsumerRecord<String, String> record) {
    var originalTopic = header(record, KafkaHeaders.DLT_ORIGINAL_TOPIC);
    // The listener's exception arrives wrapped in ListenerExecutionFailedException; the cause
    // header names what actually went wrong.
    var cause = header(record, KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN);
    var error = (cause != null ? cause : header(record, KafkaHeaders.DLT_EXCEPTION_FQCN)) + ": "
        + header(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE);
    store.add(
        originalTopic == null ? record.topic() : originalTopic,
        record.key(),
        record.value(),
        error,
        clock.instant());
  }

  private static String header(ConsumerRecord<?, ?> record, String name) {
    Header header = record.headers().lastHeader(name);
    return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
  }
}
