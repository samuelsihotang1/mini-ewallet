package io.github.samuelsihotang1.ewallet.transfer.application;

import io.github.samuelsihotang1.ewallet.transfer.domain.TransferId;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferNotFoundException;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferRepository;

/** Reads transfers for clients polling the outcome of their request. */
public class TransferQueryService {

  private final TransferRepository transfers;

  public TransferQueryService(TransferRepository transfers) {
    this.transfers = transfers;
  }

  /**
   * The transfer as it is now.
   *
   * @throws TransferNotFoundException if there is no such transfer
   */
  public TransferView get(TransferId id) {
    return transfers.findById(id).map(TransferView::of)
        .orElseThrow(() -> new TransferNotFoundException(id));
  }
}
