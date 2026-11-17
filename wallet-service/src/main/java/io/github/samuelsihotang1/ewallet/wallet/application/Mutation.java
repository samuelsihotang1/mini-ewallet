package io.github.samuelsihotang1.ewallet.wallet.application;

import io.github.samuelsihotang1.ewallet.wallet.domain.JournalId;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalType;
import io.github.samuelsihotang1.ewallet.wallet.domain.LedgerEntry.Direction;
import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import java.time.Instant;

/** One line of a wallet's transaction history: a ledger entry with its journal's context. */
public record Mutation(
    JournalId journalId,
    JournalType type,
    Direction direction,
    Money amount,
    Money balanceAfter,
    String reference,
    Instant postedAt) {}
