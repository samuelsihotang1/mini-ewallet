package io.github.samuelsihotang1.ewallet.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventCodecTest {

  private static final Instant AT = Instant.parse("2026-11-17T08:00:00.123456Z");
  private static final UUID TRANSFER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID SOURCE = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID DESTINATION = UUID.fromString("33333333-3333-3333-3333-333333333333");

  private final EventCodec codec = new EventCodec();

  @Test
  void every_transfer_event_survives_a_round_trip() {
    List<TransferEvent> events =
        List.of(
            new TransferRequested(
                UUID.randomUUID(), TRANSFER, SOURCE, DESTINATION, "150000.50", AT),
            new TransferDebited(UUID.randomUUID(), TRANSFER, DESTINATION, "150000.50", AT),
            new TransferCompensationRequested(
                UUID.randomUUID(), TRANSFER, SOURCE, "150000.50", "BALANCE_LIMIT_EXCEEDED", AT),
            new TransferCompleted(
                UUID.randomUUID(), TRANSFER, SOURCE, "user-a", DESTINATION, "user-b",
                "150000.50", AT),
            new TransferFailed(
                UUID.randomUUID(), TRANSFER, SOURCE, "user-a", DESTINATION, "user-b",
                "150000.50", "INSUFFICIENT_BALANCE", false, AT));

    for (var event : events) {
      assertThat(codec.decode(codec.encode(event), TransferEvent.class)).isEqualTo(event);
    }
  }

  @Test
  void every_wallet_event_survives_a_round_trip() {
    List<WalletEvent> events =
        List.of(
            new WalletDebited(UUID.randomUUID(), TRANSFER, SOURCE, "10.00", AT),
            new DebitFailed(UUID.randomUUID(), TRANSFER, SOURCE, "INSUFFICIENT_BALANCE", AT),
            new WalletCredited(UUID.randomUUID(), TRANSFER, DESTINATION, "10.00", AT),
            new CreditFailed(UUID.randomUUID(), TRANSFER, DESTINATION, "WALLET_NOT_FOUND", AT),
            new WalletRefunded(UUID.randomUUID(), TRANSFER, SOURCE, "10.00", AT),
            new TopUpCompleted(UUID.randomUUID(), SOURCE, "user-a", "10.00", "110.00", AT));

    for (var event : events) {
      assertThat(codec.decode(codec.encode(event), WalletEvent.class)).isEqualTo(event);
    }
  }

  @Test
  void the_json_names_the_event_type_and_keeps_the_amount_as_text() {
    var json =
        codec.encode(new WalletDebited(UUID.randomUUID(), TRANSFER, SOURCE, "0.10", AT));

    assertThat(json).contains("\"type\":\"WalletDebited\"").contains("\"amount\":\"0.10\"");
    assertThat(json).contains("\"occurredAt\":\"2026-11-17T08:00:00.123456Z\"");
  }

  @Test
  void events_of_one_transfer_share_its_partition_key() {
    var requested =
        new TransferRequested(UUID.randomUUID(), TRANSFER, SOURCE, DESTINATION, "1.00", AT);
    var debited = new WalletDebited(UUID.randomUUID(), TRANSFER, SOURCE, "1.00", AT);
    var topUp = new TopUpCompleted(UUID.randomUUID(), SOURCE, "user-a", "1.00", "1.00", AT);

    assertThat(requested.partitionKey()).isEqualTo(TRANSFER.toString());
    assertThat(debited.partitionKey()).isEqualTo(TRANSFER.toString());
    assertThat(topUp.partitionKey()).isEqualTo(SOURCE.toString());
  }

  @Test
  void an_unknown_type_is_rejected_with_the_payload_in_the_message() {
    assertThatThrownBy(
            () -> codec.decode("{\"type\":\"Nope\",\"eventId\":\"x\"}", WalletEvent.class))
        .isInstanceOf(UnreadableEventException.class)
        .hasMessageContaining("Nope");
  }

  @Test
  void a_field_added_by_a_newer_producer_is_ignored() {
    var json =
        codec.encode(new WalletDebited(UUID.randomUUID(), TRANSFER, SOURCE, "1.00", AT))
            .replace("{", "{\"addedLater\":42,");

    assertThat(codec.decode(json, WalletEvent.class)).isInstanceOf(WalletDebited.class);
  }

  @Test
  void topics_are_versioned() {
    assertThat(Topics.TRANSFER_EVENTS).isEqualTo("ewallet.transfer.events.v1");
    assertThat(Topics.WALLET_EVENTS).isEqualTo("ewallet.wallet.events.v1");
  }
}
