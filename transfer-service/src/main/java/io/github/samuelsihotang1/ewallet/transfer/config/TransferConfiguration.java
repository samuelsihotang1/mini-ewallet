package io.github.samuelsihotang1.ewallet.transfer.config;

import io.github.samuelsihotang1.ewallet.events.Topics;
import io.github.samuelsihotang1.ewallet.transfer.application.RequestTransferService;
import io.github.samuelsihotang1.ewallet.transfer.application.TransferEvents;
import io.github.samuelsihotang1.ewallet.transfer.application.TransferQueryService;
import io.github.samuelsihotang1.ewallet.transfer.application.TransferSagaService;
import io.github.samuelsihotang1.ewallet.transfer.application.WalletDirectory;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferRepository;
import io.github.samuelsihotang1.ewallet.transfer.infrastructure.HttpWalletDirectory;
import java.time.Clock;
import java.time.Duration;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

/**
 * Wires the application services by hand, so the application layer carries no Spring stereotype
 * and its unit tests build it with plain constructors.
 */
@Configuration(proxyBeanMethods = false)
public class TransferConfiguration {

  /** A transfer request waits for the wallet lookup; fail fast rather than pile up threads. */
  private static final Duration WALLET_CONNECT_TIMEOUT = Duration.ofSeconds(1);

  private static final Duration WALLET_READ_TIMEOUT = Duration.ofSeconds(2);

  @Bean
  WalletDirectory walletDirectory(
      RestClient.Builder builder, @Value("${wallet-service.base-url}") String baseUrl) {
    var requests = new SimpleClientHttpRequestFactory();
    requests.setConnectTimeout(WALLET_CONNECT_TIMEOUT);
    requests.setReadTimeout(WALLET_READ_TIMEOUT);
    return new HttpWalletDirectory(builder.baseUrl(baseUrl).requestFactory(requests).build());
  }

  @Bean
  RequestTransferService requestTransferService(
      TransferRepository transfers,
      TransferEvents events,
      WalletDirectory wallets,
      TransactionTemplate transaction,
      Clock clock) {
    return new RequestTransferService(transfers, events, wallets, transaction, clock);
  }

  @Bean
  TransferSagaService transferSagaService(
      TransferRepository transfers, TransferEvents events, Clock clock) {
    return new TransferSagaService(transfers, events, clock);
  }

  @Bean
  TransferQueryService transferQueryService(TransferRepository transfers) {
    return new TransferQueryService(transfers);
  }

  /**
   * The topic this service owns. Three partitions let three consumer instances share the load
   * while the events of one transfer, keyed by its id, stay in order on one partition.
   */
  @Bean
  NewTopic transferEventsTopic() {
    return TopicBuilder.name(Topics.TRANSFER_EVENTS).partitions(3).replicas(1).build();
  }
}
