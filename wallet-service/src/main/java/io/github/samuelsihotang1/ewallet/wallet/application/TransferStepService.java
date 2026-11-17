package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.events.CreditFailed;
import io.github.samuelsihotang1.ewallet.events.DebitFailed;
import io.github.samuelsihotang1.ewallet.events.WalletCredited;
import io.github.samuelsihotang1.ewallet.events.WalletDebited;
import io.github.samuelsihotang1.ewallet.events.WalletRefunded;
import io.github.samuelsihotang1.ewallet.wallet.domain.DomainException;
import io.github.samuelsihotang1.ewallet.wallet.domain.Journal;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalId;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalRepository;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalType;
import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.Wallet;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The wallet's steps of the transfer saga: debit the source into suspense, credit the destination
 * from it, or refund the source. Each step runs once per transfer, however often its event is
 * delivered, and answers with an event in the same transaction.
 *
 * <p>A business refusal (insufficient balance, unknown wallet, balance limit) is an answer, not an
 * error: it is announced so the transfer service can end or compensate the saga. Only a refund
 * throws, because a refund that cannot happen is a bug to retry and then investigate.
 */
public class TransferStepService {

  private final WalletRepository wallets;
  private final JournalRepository journals;
  private final WalletEvents events;
  private final Clock clock;

  public TransferStepService(
      WalletRepository wallets, JournalRepository journals, WalletEvents events, Clock clock) {
    this.wallets = wallets;
    this.journals = journals;
    this.events = events;
    this.clock = clock;
  }

  /** Takes {@code amount} from the source into suspense, announcing the outcome. */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void debit(UUID transferId, WalletId sourceId, Money amount) {
    var source = wallets.lockById(sourceId);
    if (alreadyPosted(JournalType.TRANSFER_OUT, transferId)) {
      return;
    }
    var now = now();
    if (source.isEmpty()) {
      events.publish(new DebitFailed(
          UUID.randomUUID(), transferId, sourceId.value(), "WALLET_NOT_FOUND", now));
      return;
    }
    try {
      post(source.get(), Journal.transferOut(
          JournalId.random(), source.get(), amount, transferId.toString(), now));
    } catch (DomainException refused) {
      events.publish(new DebitFailed(
          UUID.randomUUID(), transferId, sourceId.value(), refused.code(), now));
      return;
    }
    events.publish(new WalletDebited(
        UUID.randomUUID(), transferId, sourceId.value(), amount.amount().toPlainString(), now));
  }

  /** Gives {@code amount} from suspense to the destination, announcing the outcome. */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void credit(UUID transferId, WalletId destinationId, Money amount) {
    var destination = wallets.lockById(destinationId);
    if (alreadyPosted(JournalType.TRANSFER_IN, transferId)) {
      return;
    }
    var now = now();
    if (destination.isEmpty()) {
      events.publish(new CreditFailed(
          UUID.randomUUID(), transferId, destinationId.value(), "WALLET_NOT_FOUND", now));
      return;
    }
    try {
      post(destination.get(), Journal.transferIn(
          JournalId.random(), destination.get(), amount, transferId.toString(), now));
    } catch (DomainException refused) {
      events.publish(new CreditFailed(
          UUID.randomUUID(), transferId, destinationId.value(), refused.code(), now));
      return;
    }
    events.publish(new WalletCredited(
        UUID.randomUUID(), transferId, destinationId.value(), amount.amount().toPlainString(),
        now));
  }

  /**
   * Returns {@code amount} from suspense to the source.
   *
   * @throws IllegalStateException if the source wallet no longer exists
   */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void refund(UUID transferId, WalletId sourceId, Money amount) {
    Optional<Wallet> source = wallets.lockById(sourceId);
    if (alreadyPosted(JournalType.TRANSFER_REFUND, transferId)) {
      return;
    }
    var wallet = source.orElseThrow(() -> new IllegalStateException(
        "cannot refund transfer " + transferId + ": wallet " + sourceId + " is gone"));
    var now = now();
    post(wallet, Journal.transferRefund(
        JournalId.random(), wallet, amount, transferId.toString(), now));
    events.publish(new WalletRefunded(
        UUID.randomUUID(), transferId, sourceId.value(), amount.amount().toPlainString(), now));
  }

  /**
   * Checked after the wallet lock, like a top-up's key: a second copy of the event waits for the
   * first to commit, then finds its journal (READ COMMITTED lets it see that commit).
   */
  private boolean alreadyPosted(JournalType type, UUID transferId) {
    return journals.exists(type, transferId.toString());
  }

  private void post(Wallet wallet, Journal journal) {
    wallets.save(wallet);
    journals.append(journal);
  }

  private Instant now() {
    // MySQL TIMESTAMP(6) keeps microseconds; see TopUpService.
    return clock.instant().truncatedTo(ChronoUnit.MICROS);
  }
}
