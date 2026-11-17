package io.github.samuelsihotang1.ewallet.wallet.config;

import io.github.samuelsihotang1.ewallet.wallet.application.MutationReader;
import io.github.samuelsihotang1.ewallet.wallet.application.OpenWalletService;
import io.github.samuelsihotang1.ewallet.wallet.application.TopUpService;
import io.github.samuelsihotang1.ewallet.wallet.application.WalletQueryService;
import io.github.samuelsihotang1.ewallet.wallet.domain.JournalRepository;
import io.github.samuelsihotang1.ewallet.wallet.domain.TopUpRepository;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the application services by hand, so the application layer carries no Spring stereotype
 * and its unit tests build it with plain constructors.
 */
@Configuration(proxyBeanMethods = false)
public class WalletConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  OpenWalletService openWalletService(WalletRepository wallets) {
    return new OpenWalletService(wallets);
  }

  @Bean
  TopUpService topUpService(
      WalletRepository wallets, JournalRepository journals, TopUpRepository topUps, Clock clock) {
    return new TopUpService(wallets, journals, topUps, clock);
  }

  @Bean
  WalletQueryService walletQueryService(WalletRepository wallets, MutationReader mutations) {
    return new WalletQueryService(wallets, mutations);
  }
}
