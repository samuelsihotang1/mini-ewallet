package io.github.samuelsihotang1.ewallet.transfer.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import io.github.samuelsihotang1.ewallet.transfer.TestcontainersConfiguration;
import io.github.samuelsihotang1.ewallet.transfer.application.RequestTransferCommand;
import io.github.samuelsihotang1.ewallet.transfer.application.RequestTransferService;
import io.github.samuelsihotang1.ewallet.transfer.application.TransferReceipt;
import io.github.samuelsihotang1.ewallet.transfer.application.WalletDirectory;
import io.github.samuelsihotang1.ewallet.transfer.domain.Money;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** A client that retries one request on many connections at once still gets one transfer. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConcurrentRequestIT {

  @Autowired RequestTransferService requests;
  @Autowired JdbcClient jdbc;
  @MockitoBean WalletDirectory wallets;

  @Test
  void parallel_copies_of_one_request_create_one_transfer_and_one_saga() throws Exception {
    given(wallets.ownerOf(any())).willReturn(Optional.of("someone"));
    var command = new RequestTransferCommand(
        UUID.randomUUID(), UUID.randomUUID(), Money.of("10.00"), "same-key");
    var start = new CountDownLatch(1);

    var receipts = new ArrayList<TransferReceipt>();
    try (var pool = Executors.newFixedThreadPool(12)) {
      var futures = new ArrayList<Future<TransferReceipt>>();
      for (int i = 0; i < 12; i++) {
        futures.add(pool.submit(() -> {
          start.await();
          return requests.request(command);
        }));
      }
      start.countDown();
      for (var future : futures) {
        receipts.add(future.get());
      }
    }

    var id = receipts.get(0).transfer().id();
    assertThat(receipts).allSatisfy(r -> assertThat(r.transfer().id()).isEqualTo(id));
    assertThat(receipts).filteredOn(r -> !r.replayed()).hasSize(1);
    assertThat(jdbc.sql("SELECT COUNT(*) FROM outbox_event WHERE partition_key = :id")
        .param("id", id.toString()).query(Long.class).single()).isEqualTo(1);
  }
}
