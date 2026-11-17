package io.github.samuelsihotang1.ewallet.transfer;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/** Real MySQL 8 and Kafka for integration tests, wired in by {@code @ServiceConnection}. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

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
}
