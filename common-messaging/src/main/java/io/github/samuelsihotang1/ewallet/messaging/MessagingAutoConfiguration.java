package io.github.samuelsihotang1.ewallet.messaging;

import io.github.samuelsihotang1.ewallet.events.EventCodec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** Outbox, inbox and Kafka error handling for every service that has this module. */
@AutoConfiguration
public class MessagingAutoConfiguration {

  @Bean
  EventCodec eventCodec() {
    return new EventCodec();
  }

  @Bean
  Outbox outbox() {
    return new Outbox();
  }

  @Bean
  Inbox inbox() {
    return new Inbox();
  }
}
