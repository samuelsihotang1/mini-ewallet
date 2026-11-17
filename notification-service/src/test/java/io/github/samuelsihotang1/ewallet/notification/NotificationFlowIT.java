package io.github.samuelsihotang1.ewallet.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samuelsihotang1.ewallet.events.EventCodec;
import io.github.samuelsihotang1.ewallet.events.IntegrationEvent;
import io.github.samuelsihotang1.ewallet.events.TopUpCompleted;
import io.github.samuelsihotang1.ewallet.events.Topics;
import io.github.samuelsihotang1.ewallet.events.TransferCompleted;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;

/** Events in through real Kafka, notifications and dead letters out through the REST API. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "messaging.retry.initial-interval=50ms")
@Import(TestcontainersConfiguration.class)
class NotificationFlowIT {

  private static final Duration WAIT = Duration.ofSeconds(30);

  @Autowired TestRestTemplate http;
  @Autowired ObjectMapper json;
  @Autowired KafkaTemplate<String, String> kafka;
  @Autowired EventCodec codec;

  private final String alice = "user-alice-" + UUID.randomUUID();
  private final String bob = "user-bob-" + UUID.randomUUID();

  private void deliver(String topic, IntegrationEvent event) {
    kafka.send(topic, event.partitionKey(), codec.encode(event)).join();
  }

  private List<JsonNode> notificationsOf(String userId) throws Exception {
    var response = http.getForEntity(
        "/api/v1/users/{userId}/notifications", String.class, userId);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    var list = new ArrayList<JsonNode>();
    json.readTree(response.getBody()).forEach(list::add);
    return list;
  }

  @Test
  void a_completed_transfer_delivered_twice_notifies_each_side_once() throws Exception {
    var completed = new TransferCompleted(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), alice, UUID.randomUUID(), bob,
        "40000.00", Instant.now());

    deliver(Topics.TRANSFER_EVENTS, completed);
    deliver(Topics.TRANSFER_EVENTS, completed);

    await().atMost(WAIT).untilAsserted(() -> {
      assertThat(notificationsOf(alice)).singleElement()
          .satisfies(n -> assertThat(n.get("type").asText()).isEqualTo("TRANSFER_SENT"));
      assertThat(notificationsOf(bob)).singleElement()
          .satisfies(n -> assertThat(n.get("message").asText())
              .isEqualTo("You received 40000.00 IDR from " + alice + "."));
    });
  }

  @Test
  void a_top_up_notifies_the_owner() throws Exception {
    deliver(Topics.WALLET_EVENTS, new TopUpCompleted(
        UUID.randomUUID(), UUID.randomUUID(), alice, "10.00", "10.00", Instant.now()));

    await().atMost(WAIT).untilAsserted(() ->
        assertThat(notificationsOf(alice)).singleElement()
            .satisfies(n -> assertThat(n.get("type").asText()).isEqualTo("TOP_UP")));
  }

  @Test
  void an_unreadable_event_is_parked_and_listed_as_a_dead_letter() {
    var garbage = "not an event " + UUID.randomUUID();

    kafka.send(Topics.TRANSFER_EVENTS, "k", garbage).join();

    await().atMost(WAIT).untilAsserted(() -> {
      var body = json.readTree(http.getForObject("/api/v1/dead-letters", String.class));
      assertThat(body).anySatisfy(letter -> {
        assertThat(letter.get("payload").asText()).isEqualTo(garbage);
        assertThat(letter.get("originalTopic").asText()).isEqualTo(Topics.TRANSFER_EVENTS);
        assertThat(letter.get("error").asText()).contains("UnreadableEventException");
      });
    });
  }

  @Test
  void a_page_size_out_of_range_is_400() {
    var response = http.getForEntity(
        "/api/v1/users/{userId}/notifications?size=1000", String.class, alice);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
