package io.github.samuelsihotang1.ewallet.transfer.application;

import io.github.samuelsihotang1.ewallet.transfer.domain.TransferRepository;
import java.time.Clock;
import org.springframework.transaction.support.TransactionOperations;

/** Accepts transfer requests and starts their saga. */
public class RequestTransferService {

  public RequestTransferService(
      TransferRepository transfers,
      TransferEvents events,
      WalletDirectory wallets,
      TransactionOperations transaction,
      Clock clock) {}

  public TransferReceipt request(RequestTransferCommand command) {
    throw new UnsupportedOperationException("not implemented");
  }
}
