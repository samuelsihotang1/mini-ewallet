package io.github.samuelsihotang1.ewallet.transfer.api;

import io.github.samuelsihotang1.ewallet.transfer.domain.TransferId;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Typed path and header values. */
@Configuration(proxyBeanMethods = false)
class WebConfiguration implements WebMvcConfigurer {

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(String.class, TransferId.class, TransferId::of);
    registry.addConverter(String.class, IdempotencyKey.class, IdempotencyKey::new);
  }
}
