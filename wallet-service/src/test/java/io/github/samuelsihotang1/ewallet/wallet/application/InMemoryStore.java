package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.events.WalletEvent;
import io.github.samuelsihotang1.ewallet.wallet.domain.Journal;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalRepository;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalType;
import io.github.samuelsihotang1.ewallet.wallet.domain.LedgerEntry.Direction;
import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.SystemAccount;
import io.github.samuelsihotang1.ewallet.wallet.domain.TopUp;
import io.github.samuelsihotang1.ewallet.wallet.domain.TopUpRepository;
import io.github.samuelsihotang1.ewallet.wallet.domain.Wallet;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A fake of every storage port, kept in memory, for application tests without a database. */
class InMemoryStore
    implements WalletRepository, JournalRepository, TopUpRepository, MutationReader, WalletEvents {

  final Map<WalletId, Wallet> wallets = new LinkedHashMap<>();
  final List<Journal> journals = new ArrayList<>();
  final List<TopUp> topUps = new ArrayList<>();
  final List<WalletEvent> published = new ArrayList<>();

  @Override
  public void publish(WalletEvent event) {
    published.add(event);
  }

  @Override
  public boolean exists(JournalType type, String reference) {
    return journals.stream().anyMatch(j -> j.type() == type && j.reference().equals(reference));
  }

  /** Credits minus debits of the transfer suspense account over every journal. */
  Money suspenseBalance() {
    var suspense = SystemAccount.TRANSFER_SUSPENSE.id();
    return journals.stream()
        .flatMap(journal -> journal.entries().stream())
        .filter(entry -> entry.accountId().equals(suspense))
        .map(entry -> entry.direction() == Direction.CREDIT
            ? entry.amount()
            : Money.zero().minus(entry.amount()))
        .reduce(Money.zero(), Money::plus);
  }

  @Override
  public Optional<Wallet> findById(WalletId id) {
    return Optional.ofNullable(wallets.get(id));
  }

  @Override
  public Optional<Wallet> lockById(WalletId id) {
    return findById(id);
  }

  @Override
  public boolean existsByOwner(String ownerId) {
    return wallets.values().stream().anyMatch(wallet -> wallet.ownerId().equals(ownerId));
  }

  @Override
  public void save(Wallet wallet) {
    wallets.put(wallet.id(), wallet);
  }

  @Override
  public void append(Journal journal) {
    journals.add(journal);
  }

  @Override
  public Optional<TopUp> findByIdempotencyKey(WalletId walletId, String idempotencyKey) {
    return topUps.stream()
        .filter(t -> t.walletId().equals(walletId) && t.idempotencyKey().equals(idempotencyKey))
        .findFirst();
  }

  @Override
  public void save(TopUp topUp) {
    topUps.add(topUp);
  }

  @Override
  public List<Mutation> list(WalletId walletId, int page, int size) {
    return journals.stream()
        .sorted(Comparator.comparing(Journal::postedAt).reversed())
        .flatMap(
            journal ->
                journal.entries().stream()
                    .filter(entry -> entry.accountId().equals(walletId))
                    .map(
                        entry ->
                            new Mutation(
                                journal.id(),
                                journal.type(),
                                entry.direction(),
                                entry.amount(),
                                entry.balanceAfter(),
                                journal.reference(),
                                journal.postedAt())))
        .skip((long) page * size)
        .limit(size)
        .toList();
  }
}
