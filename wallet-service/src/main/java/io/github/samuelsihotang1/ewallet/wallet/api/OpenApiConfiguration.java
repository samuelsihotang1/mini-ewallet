package io.github.samuelsihotang1.ewallet.wallet.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Title and summary shown at the top of the Swagger UI. */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

  @Bean
  OpenAPI walletOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Wallet service")
                .version("v1")
                .description(
                    "Wallets with a double-entry ledger. Amounts are IDR decimal strings with at"
                        + " most two decimals. Every POST that moves money takes an"
                        + " Idempotency-Key header and is safe to retry."));
  }
}
