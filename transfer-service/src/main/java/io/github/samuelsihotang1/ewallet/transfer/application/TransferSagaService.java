package io.github.samuelsihotang1.ewallet.transfer.application;

import io.github.samuelsihotang1.ewallet.transfer.domain.TransferRepository;
import java.time.Clock;
import java.util.UUID;

/** Moves transfers along their saga as the wallet service answers. */
public class TransferSagaService {

  public TransferSagaService(TransferRepository transfers, TransferEvents events, Clock clock) {}

  public void onDebited(UUID transferId) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void onDebitFailed(UUID transferId, String reason) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void onCredited(UUID transferId) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void onCreditFailed(UUID transferId, String reason) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void onRefunded(UUID transferId) {
    throw new UnsupportedOperationException("not implemented");
  }
}
