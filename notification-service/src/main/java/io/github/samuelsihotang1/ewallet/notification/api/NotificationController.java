package io.github.samuelsihotang1.ewallet.notification.api;

import io.github.samuelsihotang1.ewallet.notification.application.NotificationQueryService;
import io.github.samuelsihotang1.ewallet.notification.domain.Notification;
import io.github.samuelsihotang1.ewallet.notification.infrastructure.deadletter.DeadLetter;
import io.github.samuelsihotang1.ewallet.notification.infrastructure.deadletter.DeadLetterStore;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** A user's notifications, and the records this service could not process. */
@RestController
@RequestMapping("/api/v1")
class NotificationController {

  private final NotificationQueryService notifications;
  private final DeadLetterStore deadLetters;

  NotificationController(NotificationQueryService notifications, DeadLetterStore deadLetters) {
    this.notifications = notifications;
    this.deadLetters = deadLetters;
  }

  @Operation(summary = "A user's notifications, newest first")
  @GetMapping("/users/{userId}/notifications")
  List<NotificationResponse> list(
      @PathVariable String userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return notifications.list(userId, page, size).stream().map(NotificationResponse::of).toList();
  }

  @Operation(
      summary = "Records parked on this service's dead-letter topics, newest first",
      description = "For operators: each was retried with backoff, or could not be parsed.")
  @GetMapping("/dead-letters")
  List<DeadLetter> deadLetters(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    if (page < 0 || size < 1 || size > NotificationQueryService.MAX_PAGE_SIZE) {
      throw new IllegalArgumentException("page must be >= 0 and size 1 to "
          + NotificationQueryService.MAX_PAGE_SIZE);
    }
    return deadLetters.list(page, size);
  }

  record NotificationResponse(
      String notificationId, String type, String message, String eventId, Instant createdAt) {

    static NotificationResponse of(Notification n) {
      return new NotificationResponse(
          n.id().toString(), n.type().name(), n.message(), n.eventId().toString(),
          n.createdAt());
    }
  }
}
