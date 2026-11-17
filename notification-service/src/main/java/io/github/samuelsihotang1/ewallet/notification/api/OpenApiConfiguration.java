package io.github.samuelsihotang1.ewallet.notification.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Title and summary shown at the top of the Swagger UI. */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

  @Bean
  OpenAPI notificationOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Notification service")
                .version("v1")
                .description(
                    "Notifications built from transfer and top-up events, and the records this"
                        + " service parked on its dead-letter topics after its retries."));
  }
}
