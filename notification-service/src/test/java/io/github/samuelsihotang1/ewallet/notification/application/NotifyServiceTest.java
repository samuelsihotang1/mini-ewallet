package io.github.samuelsihotang1.ewallet.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.samuelsihotang1.ewallet.events.TopUpCompleted;
import io.github.samuelsihotang1.ewallet.events.TransferCompleted;
import io.github.samuelsihotang1.ewallet.events.TransferFailed;
import io.github.samuelsihotang1.ewallet.events.TransferRequested;
import io.github.samuelsihotang1.ewallet.events.WalletDebited;
import io.github.samuelsihotang1.ewallet.notification.domain.Notification;
import io.github.samuelsihotang1.ewallet.notification.domain.NotificationType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotifyServiceTest {

  private static final Instant AT = Instant.parse("2026-11-17T08:00:00Z");

  private final InMemoryNotifications store = new InMemoryNotifications();
  private final NotifyService service = new NotifyService(store);
  private final UUID transfer = UUID.randomUUID();
  private final UUID alice = UUID.randomUUID();
  private final UUID bob = UUID.randomUUID();

  @Test
  void a_completed_transfer_tells_the_sender_and_the_recipient() {
    var event = new TransferCompleted(
        UUID.randomUUID(), transfer, alice, "user-alice", bob, "user-bob", "40000.00", AT);

    service.on(event);

    assertThat(store.saved)
        .extracting(Notification::userId, Notification::type, Notification::message)
        .containsExactlyInAnyOrder(
            tuple("user-alice", NotificationType.TRANSFER_SENT,
                "You sent 40000.00 IDR to user-bob."),
            tuple("user-bob", NotificationType.TRANSFER_RECEIVED,
                "You received 40000.00 IDR from user-alice."));
    assertThat(store.saved).allSatisfy(n -> {
      assertThat(n.eventId()).isEqualTo(event.eventId());
      assertThat(n.createdAt()).isEqualTo(AT);
    });
  }

  @Test
  void a_failed_transfer_tells_only_the_sender_why() {
    service.on(new TransferFailed(
        UUID.randomUUID(), transfer, alice, "user-alice", bob, "user-bob", "40000.00",
        "INSUFFICIENT_BALANCE", false, AT));

    assertThat(store.saved).singleElement().satisfies(n -> {
      assertThat(n.userId()).isEqualTo("user-alice");
      assertThat(n.type()).isEqualTo(NotificationType.TRANSFER_FAILED);
      assertThat(n.message()).isEqualTo(
          "Your transfer of 40000.00 IDR to user-bob failed: INSUFFICIENT_BALANCE.");
    });
  }

  @Test
  void a_compensated_transfer_says_the_money_is_back() {
    service.on(new TransferFailed(
        UUID.randomUUID(), transfer, alice, "user-alice", bob, "user-bob", "50.00",
        "BALANCE_LIMIT_EXCEEDED", true, AT));

    assertThat(store.saved).singleElement().satisfies(n -> assertThat(n.message()).isEqualTo(
        "Your transfer of 50.00 IDR to user-bob failed: BALANCE_LIMIT_EXCEEDED."
            + " The money is back in your wallet."));
  }

  @Test
  void a_top_up_tells_the_owner_the_new_balance() {
    service.on(new TopUpCompleted(
        UUID.randomUUID(), alice, "user-alice", "150000.00", "160000.00", AT));

    assertThat(store.saved).singleElement().satisfies(n -> {
      assertThat(n.userId()).isEqualTo("user-alice");
      assertThat(n.type()).isEqualTo(NotificationType.TOP_UP);
      assertThat(n.message())
          .isEqualTo("Top-up of 150000.00 IDR received. Balance: 160000.00 IDR.");
    });
  }

  @Test
  void saga_internals_notify_nobody() {
    service.on(new TransferRequested(UUID.randomUUID(), transfer, alice, bob, "1.00", AT));
    service.on(new WalletDebited(UUID.randomUUID(), transfer, alice, "1.00", AT));

    assertThat(store.saved).isEmpty();
  }
}
