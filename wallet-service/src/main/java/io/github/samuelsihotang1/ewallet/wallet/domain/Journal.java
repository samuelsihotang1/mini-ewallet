package io.github.samuelsihotang1.ewallet.wallet.domain;

import io.github.samuelsihotang1.ewallet.wallet.domain.LedgerEntry.Direction;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * One balanced posting to the ledger: its debits and credits always sum to the same amount, so
 * money is only ever moved between accounts, never created or destroyed. Entries are append-only;
 * a mistake is corrected by a new journal, never by editing one.
 */
public record Journal(
    JournalId id, JournalType type, String reference, Instant postedAt, List<LedgerEntry> entries) {

  public Journal {
    Objects.requireNonNull(id, "journal id");
    Objects.requireNonNull(type, "journal type");
    Objects.requireNonNull(postedAt, "posted at");
    entries = List.copyOf(entries);
    if (entries.size() < 2) {
      throw new IllegalStateException("a journal needs at least two entries, got " + entries.size());
    }
    var debits = sum(entries, Direction.DEBIT);
    var credits = sum(entries, Direction.CREDIT);
    if (!debits.equals(credits)) {
      throw new IllegalStateException(
          "unbalanced journal " + id + ": debits " + debits + " != credits " + credits);
    }
  }

  /**
   * Credits {@code wallet} with money arriving from outside, through the top-up source account.
   * Mutates {@code wallet}; the caller persists both.
   */
  public static Journal topUp(
      JournalId id, Wallet wallet, Money amount, String reference, Instant postedAt) {
    wallet.credit(amount);
    return new Journal(
        id,
        JournalType.TOP_UP,
        reference,
        postedAt,
        List.of(
            new LedgerEntry(SystemAccount.TOP_UP_SOURCE.id(), Direction.DEBIT, amount, null),
            new LedgerEntry(wallet.id(), Direction.CREDIT, amount, wallet.balance())));
  }

  public static Journal transferOut(
      JournalId id, Wallet source, Money amount, String transferId, Instant postedAt) {
    throw new UnsupportedOperationException("not implemented");
  }

  public static Journal transferIn(
      JournalId id, Wallet destination, Money amount, String transferId, Instant postedAt) {
    throw new UnsupportedOperationException("not implemented");
  }

  public static Journal transferRefund(
      JournalId id, Wallet source, Money amount, String transferId, Instant postedAt) {
    throw new UnsupportedOperationException("not implemented");
  }

  /** The sum of this journal's entries in one direction. */
  public Money total(Direction direction) {
    return sum(entries, direction);
  }

  /**
   * The entry posted to {@code accountId}.
   *
   * @throws IllegalArgumentException if this journal does not touch that account
   */
  public LedgerEntry entryFor(WalletId accountId) {
    return entries.stream()
        .filter(entry -> entry.accountId().equals(accountId))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("journal " + id + " has no entry for " + accountId));
  }

  private static Money sum(List<LedgerEntry> entries, Direction direction) {
    return entries.stream()
        .filter(entry -> entry.direction() == direction)
        .map(LedgerEntry::amount)
        .reduce(Money.zero(), Money::plus);
  }
}
