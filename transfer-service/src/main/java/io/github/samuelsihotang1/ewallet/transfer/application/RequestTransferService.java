package io.github.samuelsihotang1.ewallet.transfer.application;

import io.github.samuelsihotang1.ewallet.events.TransferRequested;
import io.github.samuelsihotang1.ewallet.transfer.domain.IdempotencyKeyReusedException;
import io.github.samuelsihotang1.ewallet.transfer.domain.Party;
import io.github.samuelsihotang1.ewallet.transfer.domain.Transfer;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferId;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferRepository;
import io.github.samuelsihotang1.ewallet.transfer.domain.WalletNotFoundException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Accepts transfer requests and starts their saga, once per source wallet and idempotency key.
 *
 * <p>The wallet service is asked, synchronously, whether both wallets exist before anything is
 * stored, so a typo in the destination fails the request at once instead of becoming a failed
 * saga a moment later. That call happens outside the database transaction: a slow wallet service
 * must not hold a connection and a row lock while it answers.
 */
public class RequestTransferService {

  private final TransferRepository transfers;
  private final TransferEvents events;
  private final WalletDirectory wallets;
  private final TransactionOperations transaction;
  private final Clock clock;

  public RequestTransferService(
      TransferRepository transfers,
      TransferEvents events,
      WalletDirectory wallets,
      TransactionOperations transaction,
      Clock clock) {
    this.transfers = transfers;
    this.events = events;
    this.wallets = wallets;
    this.transaction = transaction;
    this.clock = clock;
  }

  /**
   * Stores a PENDING transfer and asks the wallet service to debit the source, or returns the
   * earlier transfer when the source already used this key.
   *
   * @throws IdempotencyKeyReusedException if the key was used for another destination or amount
   * @throws WalletNotFoundException if either wallet does not exist
   * @throws WalletDirectoryUnavailableException if the wallet service cannot answer
   */
  public TransferReceipt request(RequestTransferCommand command) {
    Transfer.requireValid(
        command.sourceWalletId(), command.destinationWalletId(), command.amount());
    var earlier =
        transfers.findByIdempotencyKey(command.sourceWalletId(), command.idempotencyKey());
    if (earlier.isPresent()) {
      return replay(earlier.get(), command);
    }

    var source = new Party(command.sourceWalletId(), ownerOf(command.sourceWalletId()));
    var destination =
        new Party(command.destinationWalletId(), ownerOf(command.destinationWalletId()));
    // MySQL TIMESTAMP(6) keeps microseconds; cutting here keeps a replay identical.
    var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
    var transfer = Transfer.request(
        TransferId.random(), source, destination, command.amount(), command.idempotencyKey(), now);
    try {
      transaction.executeWithoutResult(status -> {
        transfers.add(transfer);
        events.publish(new TransferRequested(
            UUID.randomUUID(),
            transfer.id().value(),
            source.walletId(),
            destination.walletId(),
            transfer.amount().amount().toPlainString(),
            now));
      });
    } catch (DuplicateKeyException concurrentRetry) {
      // Two copies of one request passed the lookup above at once; UNIQUE(source, key) let one
      // in. The loser answers with the winner's transfer.
      var winner = transfers
          .findByIdempotencyKey(command.sourceWalletId(), command.idempotencyKey())
          .orElseThrow(() -> concurrentRetry);
      return replay(winner, command);
    }
    return new TransferReceipt(TransferView.of(transfer), false);
  }

  private TransferReceipt replay(Transfer earlier, RequestTransferCommand command) {
    if (!earlier.sameRequestAs(command.destinationWalletId(), command.amount())) {
      throw new IdempotencyKeyReusedException(command.idempotencyKey());
    }
    return new TransferReceipt(TransferView.of(earlier), true);
  }

  private String ownerOf(UUID walletId) {
    return wallets.ownerOf(walletId).orElseThrow(() -> new WalletNotFoundException(walletId));
  }
}
