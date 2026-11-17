package io.github.samuelsihotang1.ewallet.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.samuelsihotang1.ewallet.notification.domain.Notification;
import io.github.samuelsihotang1.ewallet.notification.domain.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationQueryServiceTest {

  private final InMemoryNotifications store = new InMemoryNotifications();
  private final NotificationQueryService queries = new NotificationQueryService(store);

  private static Notification at(String userId, String instant) {
    return new Notification(UUID.randomUUID(), userId, NotificationType.TOP_UP, "m",
        UUID.randomUUID(), Instant.parse(instant));
  }

  @Test
  void a_user_sees_only_their_notifications_newest_first() {
    store.saveAll(List.of(
        at("u1", "2026-11-17T08:00:00Z"),
        at("u2", "2026-11-17T08:00:01Z"),
        at("u1", "2026-11-17T08:00:02Z")));

    var page = queries.list("u1", 0, 10);

    assertThat(page).extracting(n -> n.createdAt().toString())
        .containsExactly("2026-11-17T08:00:02Z", "2026-11-17T08:00:00Z");
  }

  @Test
  void pages_are_bounded() {
    assertThatThrownBy(() -> queries.list("u1", -1, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> queries.list("u1", 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> queries.list("u1", 0, NotificationQueryService.MAX_PAGE_SIZE + 1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
