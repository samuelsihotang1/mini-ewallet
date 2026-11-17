package io.github.samuelsihotang1.ewallet.wallet.domain;

/** Append-only storage of ledger journals. */
public interface JournalRepository {

  /** Appends the journal and all its entries; never updates an existing one. */
  void append(Journal journal);
}
