package io.github.samuelsihotang1.ewallet.wallet.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * An exact amount of Indonesian rupiah with two decimals.
 *
 * <p>The amount is always stored at scale 2, so the record's own {@code equals} is safe:
 * {@code 1.5} and {@code 1.50} become the same value. Input with more than two decimals is
 * rejected, never rounded: a payment system must not invent or lose a fraction of a rupiah.
 * The value may be negative (a result of {@link #minus}); rules about sign belong to the caller.
 */
public record Money(BigDecimal amount, Currency currency) {

  /** The only currency this wallet holds. */
  public static final Currency IDR = Currency.getInstance("IDR");

  private static final int SCALE = 2;
  private static final Money ZERO = new Money(BigDecimal.ZERO.setScale(SCALE), IDR);

  public Money {
    if (amount == null) {
      throw new InvalidMoneyException("amount is required");
    }
    if (!IDR.equals(currency)) {
      throw new InvalidMoneyException("unsupported currency " + currency + ", only IDR");
    }
    try {
      amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException tooManyDecimals) {
      throw new InvalidMoneyException(
          "amount " + amount.toPlainString() + " has more than " + SCALE + " decimals");
    }
  }

  /** Parses a decimal string such as {@code "150000"} or {@code "150000.50"} as rupiah. */
  public static Money of(String amount) {
    if (amount == null) {
      throw new InvalidMoneyException("amount is required");
    }
    try {
      return new Money(new BigDecimal(amount.trim()), IDR);
    } catch (NumberFormatException notANumber) {
      throw new InvalidMoneyException("amount '" + amount + "' is not a decimal number");
    }
  }

  /** Builds money from an amount and currency; anything but IDR is rejected. */
  public static Money of(BigDecimal amount, Currency currency) {
    return new Money(amount, currency);
  }

  /** Zero rupiah. */
  public static Money zero() {
    return ZERO;
  }

  /** The exact sum. */
  public Money plus(Money other) {
    return new Money(amount.add(other.amount), IDR);
  }

  /** The exact difference; may be negative. */
  public Money minus(Money other) {
    return new Money(amount.subtract(other.amount), IDR);
  }

  /** Whether the amount is strictly above zero. */
  public boolean isPositive() {
    return amount.signum() > 0;
  }

  /** Whether the amount is strictly below zero. */
  public boolean isNegative() {
    return amount.signum() < 0;
  }

  /** Whether this amount is strictly larger than {@code other}. */
  public boolean isGreaterThan(Money other) {
    return amount.compareTo(other.amount) > 0;
  }

  /** Whether this amount is strictly smaller than {@code other}. */
  public boolean isLessThan(Money other) {
    return amount.compareTo(other.amount) < 0;
  }

  @Override
  public String toString() {
    return amount.toPlainString() + " " + currency.getCurrencyCode();
  }
}
