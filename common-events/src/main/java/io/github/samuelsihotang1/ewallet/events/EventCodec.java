package io.github.samuelsihotang1.ewallet.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Turns events into JSON and back; one shared format for every producer and consumer. Thread-safe.
 *
 * <p>Unknown fields are ignored so a consumer keeps working when a producer adds one; an unknown
 * {@code type} is an {@link UnreadableEventException}, which consumers send to a dead-letter topic
 * instead of retrying.
 */
public class EventCodec {

  private final ObjectMapper json =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  /** The event as JSON, including its {@code type} name. */
  public String encode(IntegrationEvent event) {
    try {
      return json.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("cannot encode " + event, e);
    }
  }

  /**
   * Reads one event of the {@code type} family, such as {@code WalletEvent.class}.
   *
   * @throws UnreadableEventException if the JSON is malformed or names an unknown type
   */
  public <T extends IntegrationEvent> T decode(String payload, Class<T> type) {
    try {
      return json.readValue(payload, type);
    } catch (JsonProcessingException e) {
      throw new UnreadableEventException("unreadable " + type.getSimpleName() + ": " + payload, e);
    }
  }
}
