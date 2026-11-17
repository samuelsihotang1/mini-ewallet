package io.github.samuelsihotang1.ewallet.events;

import java.util.UUID;

/** A wallet event answering one step of a transfer saga; keyed by the transfer. */
public sealed interface TransferStepEvent extends WalletEvent
    permits WalletDebited, DebitFailed, WalletCredited, CreditFailed, WalletRefunded {

  /** The transfer whose step this answers. */
  UUID transferId();

  @Override
  default String partitionKey() {
    return transferId().toString();
  }
}
