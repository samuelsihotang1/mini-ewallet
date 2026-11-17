package io.github.samuelsihotang1.ewallet.transfer.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Title and summary shown at the top of the Swagger UI. */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

  @Bean
  OpenAPI transferOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Transfer service")
                .version("v1")
                .description(
                    "Wallet-to-wallet transfers run as a saga over Kafka. A request is accepted"
                        + " with 202 and finishes asynchronously; poll the transfer for its"
                        + " status. Amounts are IDR decimal strings with at most two decimals."));
  }
}
