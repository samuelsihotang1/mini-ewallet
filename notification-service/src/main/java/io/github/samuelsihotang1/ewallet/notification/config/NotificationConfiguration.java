package io.github.samuelsihotang1.ewallet.notification.config;

import io.github.samuelsihotang1.ewallet.events.Topics;
import io.github.samuelsihotang1.ewallet.notification.application.NotificationQueryService;
import io.github.samuelsihotang1.ewallet.notification.application.NotifyService;
import io.github.samuelsihotang1.ewallet.notification.domain.NotificationRepository;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Wires the application services by hand, and creates this service's dead-letter topics so their
 * listener can subscribe before the first record is parked.
 */
@Configuration(proxyBeanMethods = false)
public class NotificationConfiguration {

  @Bean
  NotifyService notifyService(NotificationRepository notifications) {
    return new NotifyService(notifications);
  }

  @Bean
  NotificationQueryService notificationQueryService(NotificationRepository notifications) {
    return new NotificationQueryService(notifications);
  }

  @Bean
  NewTopic transferDeadLetters(@Value("${messaging.retry.dead-letter-suffix}") String suffix) {
    return TopicBuilder.name(Topics.TRANSFER_EVENTS + suffix).partitions(1).replicas(1).build();
  }

  @Bean
  NewTopic walletDeadLetters(@Value("${messaging.retry.dead-letter-suffix}") String suffix) {
    return TopicBuilder.name(Topics.WALLET_EVENTS + suffix).partitions(1).replicas(1).build();
  }
}
