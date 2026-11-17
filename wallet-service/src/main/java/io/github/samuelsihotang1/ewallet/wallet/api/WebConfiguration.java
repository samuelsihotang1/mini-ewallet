package io.github.samuelsihotang1.ewallet.wallet.api;

import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Converts path and header text into typed values, so controllers never see a raw string id. */
@Configuration(proxyBeanMethods = false)
class WebConfiguration implements WebMvcConfigurer {

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(String.class, WalletId.class, WalletId::of);
    registry.addConverter(String.class, IdempotencyKey.class, IdempotencyKey::new);
  }
}
