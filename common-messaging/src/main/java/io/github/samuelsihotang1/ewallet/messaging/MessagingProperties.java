package io.github.samuelsihotang1.ewallet.messaging;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tuning of the outbox relay and the consumer retry policy, under {@code messaging.*}.
 *
 * @param outbox the relay: how often it polls, how many rows per batch, how long to wait for Kafka
 * @param retry how often a failing record is tried before it is parked on its dead-letter topic
 */
@ConfigurationProperties("messaging")
public record MessagingProperties(@DefaultValue Outbox outbox, @DefaultValue Retry retry) {

  /**
   * @param enabled false turns the relay off, for a service that only consumes
   * @param pollIntervalMs pause between polls when nothing is pending
   */
  public record Outbox(
      @DefaultValue("true") boolean enabled,
      @DefaultValue("100") long pollIntervalMs,
      @DefaultValue("100") int batchSize,
      @DefaultValue("10s") Duration sendTimeout) {}

  /**
   * @param maxAttempts total deliveries of one record, the first included
   * @param initialInterval wait before the first retry; doubles on each further retry
   */
  public record Retry(
      @DefaultValue("4") int maxAttempts, @DefaultValue("500ms") Duration initialInterval) {}
}
