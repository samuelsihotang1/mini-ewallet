package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.wallet.domain.IdempotencyKeyReusedException;
import io.github.samuelsihotang1.ewallet.wallet.domain.Journal;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalId;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalRepository;
import io.github.samuelsihotang1.ewallet.wallet.domain.TopUp;
import io.github.samuelsihotang1.ewallet.wallet.domain.TopUpRepository;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletNotFoundException;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletRepository;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Adds money to a wallet exactly once per idempotency key. */
public class TopUpService {

  private final WalletRepository wallets;
  private final JournalRepository journals;
  private final TopUpRepository topUps;
  private final Clock clock;

  public TopUpService(
      WalletRepository wallets, JournalRepository journals, TopUpRepository topUps, Clock clock) {
    this.wallets = wallets;
    this.journals = journals;
    this.topUps = topUps;
    this.clock = clock;
  }

  /**
   * Credits the wallet, or returns the stored receipt when this wallet already saw the key.
   *
   * <p>The wallet row is locked before the key is looked up, so two requests with the same key
   * run one after the other and the second always finds the first. READ COMMITTED lets that
   * lookup see the row the first request committed while this one waited for the lock; under
   * MySQL's default REPEATABLE READ it could read an older snapshot.
   *
   * @throws WalletNotFoundException if the wallet does not exist
   * @throws IdempotencyKeyReusedException if the key was used for a different amount
   */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public TopUpReceipt topUp(TopUpCommand command) {
    TopUp.requireAllowed(command.amount());
    var wallet =
        wallets
            .lockById(command.walletId())
            .orElseThrow(() -> new WalletNotFoundException(command.walletId()));

    var earlier = topUps.findByIdempotencyKey(wallet.id(), command.idempotencyKey());
    if (earlier.isPresent()) {
      if (!earlier.get().sameRequestAs(wallet.id(), command.amount())) {
        throw new IdempotencyKeyReusedException(command.idempotencyKey());
      }
      return TopUpReceipt.of(earlier.get(), true);
    }

    // MySQL TIMESTAMP(6) rounds anything finer than microseconds; cutting first keeps the
    // replayed receipt identical to the first one.
    var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
    var topUpId = UUID.randomUUID();
    var journal =
        Journal.topUp(JournalId.random(), wallet, command.amount(), topUpId.toString(), now);
    var topUp =
        new TopUp(
            topUpId,
            wallet.id(),
            command.idempotencyKey(),
            command.amount(),
            journal.id(),
            wallet.balance(),
            now);
    wallets.save(wallet);
    journals.append(journal);
    topUps.save(topUp);
    return TopUpReceipt.of(topUp, false);
  }
}
