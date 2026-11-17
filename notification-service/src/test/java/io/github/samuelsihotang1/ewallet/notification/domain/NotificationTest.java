package io.github.samuelsihotang1.ewallet.notification.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest {

  @Test
  void a_notification_needs_a_recipient() {
    assertThatThrownBy(() -> new Notification(UUID.randomUUID(), " ", NotificationType.TOP_UP,
            "hello", UUID.randomUUID(), Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void a_notification_needs_a_message() {
    assertThatThrownBy(() -> new Notification(UUID.randomUUID(), "u1", NotificationType.TOP_UP,
            "", UUID.randomUUID(), Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
