package io.github.samuelsihotang1.ewallet.notification.infrastructure.deadletter;

import java.time.Instant;

/**
 * A record this service gave up on, kept for an operator to inspect and replay by hand.
 *
 * @param error the exception class and message that made the last attempt fail
 */
public record DeadLetter(
    long id, String originalTopic, String key, String payload, String error, Instant receivedAt) {}
