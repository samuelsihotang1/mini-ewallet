package io.github.samuelsihotang1.ewallet.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;

/** A change in a transfer's state, published on {@link Topics#TRANSFER_EVENTS}. */
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(TransferRequested.class),
  @JsonSubTypes.Type(TransferDebited.class),
  @JsonSubTypes.Type(TransferCompensationRequested.class),
  @JsonSubTypes.Type(TransferCompleted.class),
  @JsonSubTypes.Type(TransferFailed.class)
})
public sealed interface TransferEvent extends IntegrationEvent
    permits TransferRequested,
        TransferDebited,
        TransferCompensationRequested,
        TransferCompleted,
        TransferFailed {

  /** The transfer this event belongs to. */
  UUID transferId();

  @Override
  default String partitionKey() {
    return transferId().toString();
  }
}
