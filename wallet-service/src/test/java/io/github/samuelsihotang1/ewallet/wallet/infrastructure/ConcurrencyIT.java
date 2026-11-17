package io.github.samuelsihotang1.ewallet.wallet.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.samuelsihotang1.ewallet.wallet.TestcontainersConfiguration;
import io.github.samuelsihotang1.ewallet.wallet.application.OpenWalletService;
import io.github.samuelsihotang1.ewallet.wallet.application.TopUpCommand;
import io.github.samuelsihotang1.ewallet.wallet.application.TopUpReceipt;
import io.github.samuelsihotang1.ewallet.wallet.application.TopUpService;
import io.github.samuelsihotang1.ewallet.wallet.application.TransferStepService;
import io.github.samuelsihotang1.ewallet.wallet.application.WalletQueryService;
import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.SystemAccount;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Many requests released at the same instant against real MySQL. The assertions are the money
 * invariants, never timing: no lost update, no overdraft, one effect per idempotency key or
 * transfer, every wallet balance equal to its ledger, suspense back to zero.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConcurrencyIT {

  private static final int THREADS = 16;

  @Autowired OpenWalletService openWallet;
  @Autowired TopUpService topUps;
  @Autowired TransferStepService steps;
  @Autowired WalletQueryService queries;
  @Autowired JdbcClient jdbc;

  /** Runs {@code tasks} tasks on {@link #THREADS} threads, all released by one start signal. */
  private static <T> List<T> race(int tasks, IntFunction<Callable<T>> task) throws Exception {
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(THREADS)) {
      var futures = new ArrayList<Future<T>>();
      for (int i = 0; i < tasks; i++) {
        var work = task.apply(i);
        futures.add(pool.submit(() -> {
          start.await();
          return work.call();
        }));
      }
      start.countDown();
      var results = new ArrayList<T>();
      for (var future : futures) {
        results.add(future.get());
      }
      return results;
    }
  }

  private WalletId walletWith(String balance) {
    var wallet = openWallet.open("user-" + UUID.randomUUID());
    topUps.topUp(new TopUpCommand(wallet.id(), Money.of(balance), "seed"));
    return wallet.id();
  }

  private Money balanceOf(WalletId id) {
    return queries.get(id).balance();
  }

  private BigDecimal ledgerBalanceOf(WalletId id) {
    return jdbc.sql("SELECT COALESCE(SUM(CASE direction WHEN 'CREDIT' THEN amount"
            + " ELSE -amount END), 0) FROM ledger_entry WHERE account_id = :id")
        .param("id", id.toString())
        .query(BigDecimal.class)
        .single();
  }

  private long journals(String type, String reference) {
    return jdbc.sql("SELECT COUNT(*) FROM journal WHERE type = :type AND reference = :ref")
        .param("type", type)
        .param("ref", reference)
        .query(Long.class)
        .single();
  }

  @Test
  void parallel_top_ups_with_different_keys_all_land() throws Exception {
    var wallet = walletWith("0.01");

    race(50, i -> () -> topUps.topUp(new TopUpCommand(wallet, Money.of("1.00"), "k-" + i)));

    assertThat(balanceOf(wallet)).isEqualTo(Money.of("50.01"));
    assertThat(ledgerBalanceOf(wallet)).isEqualByComparingTo("50.01");
  }

  @Test
  void parallel_retries_of_one_key_credit_once_and_agree_on_the_receipt() throws Exception {
    var wallet = walletWith("0.01");

    List<TopUpReceipt> receipts =
        race(20, i -> () -> topUps.topUp(new TopUpCommand(wallet, Money.of("10.00"), "same")));

    assertThat(balanceOf(wallet)).isEqualTo(Money.of("10.01"));
    assertThat(receipts).extracting(TopUpReceipt::topUpId).containsOnly(receipts.get(0).topUpId());
    assertThat(receipts).filteredOn(r -> !r.replayed()).hasSize(1);
  }

  @Test
  void parallel_debits_never_overdraw_the_wallet() throws Exception {
    var wallet = walletWith("100.00");

    race(30, i -> () -> {
      steps.debit(UUID.randomUUID(), wallet, Money.of("10.00"));
      return null;
    });

    assertThat(balanceOf(wallet)).isEqualTo(Money.zero());
    assertThat(ledgerBalanceOf(wallet)).isEqualByComparingTo("0.00");
    assertThat(outboxCount(wallet, "WalletDebited")).isEqualTo(10);
    assertThat(outboxCount(wallet, "DebitFailed")).isEqualTo(20);
  }

  @Test
  void one_debit_delivered_to_many_threads_at_once_is_posted_once() throws Exception {
    var wallet = walletWith("100.00");
    var transferId = UUID.randomUUID();

    race(10, i -> () -> {
      steps.debit(transferId, wallet, Money.of("10.00"));
      return null;
    });

    assertThat(balanceOf(wallet)).isEqualTo(Money.of("90.00"));
    assertThat(journals("TRANSFER_OUT", transferId.toString())).isEqualTo(1);
  }

  @Test
  void transfers_in_both_directions_at_once_neither_deadlock_nor_lose_money() throws Exception {
    var a = walletWith("1000.00");
    var b = walletWith("1000.00");
    var transferIds = new ArrayList<UUID>();

    race(40, i -> {
      var transferId = UUID.randomUUID();
      synchronized (transferIds) {
        transferIds.add(transferId);
      }
      var from = i % 2 == 0 ? a : b;
      var to = i % 2 == 0 ? b : a;
      return () -> {
        steps.debit(transferId, from, Money.of("7.00"));
        steps.credit(transferId, to, Money.of("7.00"));
        return null;
      };
    });

    assertThat(balanceOf(a).plus(balanceOf(b))).isEqualTo(Money.of("2000.00"));
    assertThat(balanceOf(a)).isEqualTo(Money.of("1000.00"));
    assertThat(suspenseOf(transferIds)).isEqualByComparingTo("0.00");
  }

  private long outboxCount(WalletId wallet, String type) {
    return jdbc.sql("SELECT COUNT(*) FROM outbox_event WHERE event_type = :type"
            + " AND JSON_UNQUOTE(JSON_EXTRACT(payload, '$.walletId')) = :wallet")
        .param("type", type)
        .param("wallet", wallet.toString())
        .query(Long.class)
        .single();
  }

  private BigDecimal suspenseOf(List<UUID> transferIds) {
    return jdbc.sql("SELECT COALESCE(SUM(CASE e.direction WHEN 'CREDIT' THEN e.amount"
            + " ELSE -e.amount END), 0) FROM ledger_entry e JOIN journal j ON j.id = e.journal_id"
            + " WHERE e.account_id = :suspense AND j.reference IN (:refs)")
        .param("suspense", SystemAccount.TRANSFER_SUSPENSE.id().toString())
        .param("refs", transferIds.stream().map(UUID::toString).toList())
        .query(BigDecimal.class)
        .single();
  }
}
