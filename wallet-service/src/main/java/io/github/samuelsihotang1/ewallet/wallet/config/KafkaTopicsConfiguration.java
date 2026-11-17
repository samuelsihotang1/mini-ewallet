package io.github.samuelsihotang1.ewallet.wallet.config;

import io.github.samuelsihotang1.ewallet.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Creates the topic this service owns. Three partitions let three consumer instances share the
 * load while events of one transfer, keyed by its id, stay in order on one partition.
 */
@Configuration(proxyBeanMethods = false)
public class KafkaTopicsConfiguration {

  @Bean
  NewTopic walletEventsTopic() {
    return TopicBuilder.name(Topics.WALLET_EVENTS).partitions(3).replicas(1).build();
  }
}
