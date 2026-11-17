package io.github.samuelsihotang1.ewallet.events;

/** Turns events into JSON and back; one shared format for every producer and consumer. */
public class EventCodec {

  public String encode(IntegrationEvent event) {
    throw new UnsupportedOperationException("not implemented");
  }

  public <T extends IntegrationEvent> T decode(String json, Class<T> type) {
    throw new UnsupportedOperationException("not implemented");
  }
}
