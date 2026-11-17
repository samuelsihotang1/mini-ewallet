package io.github.samuelsihotang1.ewallet.wallet.domain;

import java.math.BigDecimal;
import java.util.Currency;

/** An exact amount of Indonesian rupiah with two decimals. */
public record Money(BigDecimal amount, Currency currency) {

  public static Money of(String amount) {
    throw new UnsupportedOperationException("not implemented");
  }

  public static Money of(BigDecimal amount, Currency currency) {
    throw new UnsupportedOperationException("not implemented");
  }

  public static Money zero() {
    throw new UnsupportedOperationException("not implemented");
  }

  public Money plus(Money other) {
    throw new UnsupportedOperationException("not implemented");
  }

  public Money minus(Money other) {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean isPositive() {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean isNegative() {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean isGreaterThan(Money other) {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean isLessThan(Money other) {
    throw new UnsupportedOperationException("not implemented");
  }
}
