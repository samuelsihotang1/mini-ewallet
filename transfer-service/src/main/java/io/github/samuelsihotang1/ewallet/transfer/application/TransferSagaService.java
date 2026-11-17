package io.github.samuelsihotang1.ewallet.transfer.application;

import io.github.samuelsihotang1.ewallet.events.TransferCompensationRequested;
import io.github.samuelsihotang1.ewallet.events.TransferCompleted;
import io.github.samuelsihotang1.ewallet.events.TransferDebited;
import io.github.samuelsihotang1.ewallet.events.TransferEvent;
import io.github.samuelsihotang1.ewallet.events.TransferFailed;
import io.github.samuelsihotang1.ewallet.transfer.domain.Transfer;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferId;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves transfers along their saga as the wallet service answers (choreography: no central
 * coordinator calls the wallet; each side reacts to the other's events). Each reaction locks the
 * transfer, moves its state and writes the next event to the outbox in one transaction.
 */
public class TransferSagaService {

  private final TransferRepository transfers;
  private final TransferEvents events;
  private final Clock clock;

  public TransferSagaService(TransferRepository transfers, TransferEvents events, Clock clock) {
    this.transfers = transfers;
    this.events = events;
    this.clock = clock;
  }

  /** The source was debited: ask for the destination to be credited. */
  @Transactional
  public void onDebited(UUID transferId) {
    react(transferId, (t, now) -> t.debited(now), t -> new TransferDebited(
        UUID.randomUUID(), t.id().value(), t.destination().walletId(), amountOf(t), now()));
  }

  /** The source could not be debited: the transfer failed, nothing to undo. */
  @Transactional
  public void onDebitFailed(UUID transferId, String reason) {
    react(transferId, (t, now) -> t.debitFailed(reason, now), t -> failed(t, false));
  }

  /** The destination was credited: the transfer is complete. */
  @Transactional
  public void onCredited(UUID transferId) {
    react(transferId, (t, now) -> t.credited(now), t -> new TransferCompleted(
        UUID.randomUUID(), t.id().value(),
        t.source().walletId(), t.source().ownerId(),
        t.destination().walletId(), t.destination().ownerId(),
        amountOf(t), now()));
  }

  /** The destination refused the money: ask for the source to be refunded. */
  @Transactional
  public void onCreditFailed(UUID transferId, String reason) {
    react(transferId, (t, now) -> t.creditFailed(reason, now), t ->
        new TransferCompensationRequested(
            UUID.randomUUID(), t.id().value(), t.source().walletId(), amountOf(t), reason, now()));
  }

  /** The source has its money back: the transfer failed and was compensated. */
  @Transactional
  public void onRefunded(UUID transferId) {
    react(transferId, (t, now) -> t.refunded(now), t -> failed(t, true));
  }

  /**
   * Applies {@code step}; if it moved the transfer on, saves it and publishes {@code next}.
   *
   * @throws IllegalStateException for an unknown transfer: the wallet only answers transfers
   *     this service created, so this is a bug, and the record goes to the dead-letter topic
   */
  private void react(
      UUID transferId,
      BiPredicate<Transfer, Instant> step,
      Function<Transfer, TransferEvent> next) {
    var transfer = transfers.lockById(new TransferId(transferId))
        .orElseThrow(() -> new IllegalStateException("no transfer " + transferId));
    if (step.test(transfer, now())) {
      transfers.save(transfer);
      events.publish(next.apply(transfer));
    }
  }

  private TransferFailed failed(Transfer t, boolean compensated) {
    return new TransferFailed(
        UUID.randomUUID(), t.id().value(),
        t.source().walletId(), t.source().ownerId(),
        t.destination().walletId(), t.destination().ownerId(),
        amountOf(t), t.failureReason(), compensated, now());
  }

  private static String amountOf(Transfer transfer) {
    return transfer.amount().amount().toPlainString();
  }

  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS);
  }
}
