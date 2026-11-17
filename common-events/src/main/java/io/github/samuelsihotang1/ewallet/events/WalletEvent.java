package io.github.samuelsihotang1.ewallet.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Money that moved, or could not move, in a wallet; published on {@link Topics#WALLET_EVENTS}. */
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(WalletDebited.class),
  @JsonSubTypes.Type(DebitFailed.class),
  @JsonSubTypes.Type(WalletCredited.class),
  @JsonSubTypes.Type(CreditFailed.class),
  @JsonSubTypes.Type(WalletRefunded.class),
  @JsonSubTypes.Type(TopUpCompleted.class)
})
public sealed interface WalletEvent extends IntegrationEvent
    permits TransferStepEvent, TopUpCompleted {}
