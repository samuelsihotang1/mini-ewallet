package io.github.samuelsihotang1.ewallet.wallet.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.samuelsihotang1.ewallet.wallet.TestcontainersConfiguration;
import io.github.samuelsihotang1.ewallet.wallet.application.Mutation;
import io.github.samuelsihotang1.ewallet.wallet.application.OpenWalletService;
import io.github.samuelsihotang1.ewallet.wallet.application.TopUpCommand;
import io.github.samuelsihotang1.ewallet.wallet.application.TopUpService;
import io.github.samuelsihotang1.ewallet.wallet.application.WalletQueryService;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalType;
import io.github.samuelsihotang1.ewallet.wallet.domain.LedgerEntry.Direction;
import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.SystemAccount;
import io.github.samuelsihotang1.ewallet.wallet.domain.Wallet;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WalletPersistenceIT {

  @Autowired OpenWalletService openWallet;
  @Autowired TopUpService topUps;
  @Autowired WalletQueryService queries;
  @Autowired WalletRepository wallets;
  @Autowired JdbcClient jdbc;

  private static String newUser() {
    return "user-" + UUID.randomUUID();
  }

  @Test
  void an_opened_wallet_is_stored_with_a_zero_balance_in_idr() {
    var opened = openWallet.open(newUser());

    var stored = wallets.findById(opened.id()).orElseThrow();

    assertThat(stored.ownerId()).isEqualTo(opened.ownerId());
    assertThat(stored.balance()).isEqualTo(Money.zero());
  }

  @Test
  void the_database_rejects_a_second_wallet_for_the_same_owner() {
    var owner = newUser();
    wallets.save(Wallet.open(WalletId.random(), owner));

    assertThatThrownBy(() -> wallets.save(Wallet.open(WalletId.random(), owner)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void a_top_up_survives_the_round_trip_and_the_ledger_matches_the_balance() {
    var wallet = openWallet.open(newUser());

    topUps.topUp(new TopUpCommand(wallet.id(), Money.of("150000.50"), "key-1"));
    topUps.topUp(new TopUpCommand(wallet.id(), Money.of("49999.50"), "key-2"));

    assertThat(queries.get(wallet.id()).balance()).isEqualTo(Money.of("200000.00"));
    assertThat(ledgerBalanceOf(wallet.id())).isEqualByComparingTo("200000.00");
  }

  @Test
  void every_stored_journal_balances() {
    var wallet = openWallet.open(newUser());
    topUps.topUp(new TopUpCommand(wallet.id(), Money.of("1000"), "key-1"));

    var unbalanced =
        jdbc.sql(
                """
                SELECT COUNT(*) FROM (
                  SELECT journal_id
                  FROM ledger_entry
                  GROUP BY journal_id
                  HAVING SUM(CASE direction WHEN 'DEBIT' THEN amount ELSE -amount END) <> 0
                ) unbalanced
                """)
            .query(Long.class)
            .single();

    assertThat(unbalanced).isZero();
  }

  @Test
  void a_replayed_top_up_is_stored_once() {
    var wallet = openWallet.open(newUser());
    var command = new TopUpCommand(wallet.id(), Money.of("25000"), "same-key");

    var first = topUps.topUp(command);
    var replay = topUps.topUp(command);

    assertThat(replay.replayed()).isTrue();
    assertThat(replay.topUpId()).isEqualTo(first.topUpId());
    assertThat(replay.balanceAfter()).isEqualTo(Money.of("25000"));
    assertThat(queries.get(wallet.id()).balance()).isEqualTo(Money.of("25000"));
  }

  @Test
  void history_reads_the_ledger_newest_first() {
    var wallet = openWallet.open(newUser());
    topUps.topUp(new TopUpCommand(wallet.id(), Money.of("1000"), "key-1"));
    topUps.topUp(new TopUpCommand(wallet.id(), Money.of("2000"), "key-2"));

    var history = queries.mutations(wallet.id(), 0, 10);

    assertThat(history).extracting(Mutation::amount)
        .containsExactly(Money.of("2000"), Money.of("1000"));
    assertThat(history).extracting(Mutation::type).containsOnly(JournalType.TOP_UP);
    assertThat(history).extracting(Mutation::direction).containsOnly(Direction.CREDIT);
    assertThat(history.getFirst().balanceAfter()).isEqualTo(Money.of("3000"));
    assertThat(queries.mutations(wallet.id(), 1, 1)).extracting(Mutation::amount)
        .containsExactly(Money.of("1000"));
  }

  @Test
  void the_system_top_up_source_mirrors_every_top_up() {
    var wallet = openWallet.open(newUser());
    var before = sourceDebits();

    topUps.topUp(new TopUpCommand(wallet.id(), Money.of("777"), "key-1"));

    assertThat(sourceDebits().subtract(before)).isEqualByComparingTo("777");
  }

  private BigDecimal ledgerBalanceOf(WalletId walletId) {
    return jdbc.sql(
            """
            SELECT COALESCE(SUM(CASE direction WHEN 'CREDIT' THEN amount ELSE -amount END), 0)
            FROM ledger_entry WHERE account_id = :account
            """)
        .param("account", walletId.toString())
        .query(BigDecimal.class)
        .single();
  }

  private BigDecimal sourceDebits() {
    return jdbc.sql(
            "SELECT COALESCE(SUM(amount), 0) FROM ledger_entry"
                + " WHERE account_id = :account AND direction = 'DEBIT'")
        .param("account", SystemAccount.TOP_UP_SOURCE.id().toString())
        .query(BigDecimal.class)
        .single();
  }
}
