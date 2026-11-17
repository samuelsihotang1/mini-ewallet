package io.github.samuelsihotang1.ewallet.transfer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

  @Test
  void amounts_are_normalized_to_two_decimals_in_idr() {
    var money = Money.of("150000");

    assertThat(money.amount()).isEqualTo(new BigDecimal("150000.00"));
    assertThat(money.currency()).isEqualTo(Currency.getInstance("IDR"));
  }

  @Test
  void same_value_with_different_scale_is_equal() {
    assertThat(Money.of("1.5")).isEqualTo(Money.of("1.50"));
    assertThat(Money.of("1.5")).hasSameHashCodeAs(Money.of("1.50"));
  }

  @Test
  void more_than_two_decimals_is_rejected_instead_of_rounded() {
    assertThatThrownBy(() -> Money.of("10.005"))
        .isInstanceOf(InvalidMoneyException.class)
        .hasMessageContaining("10.005");
  }

  @Test
  void a_currency_other_than_idr_is_rejected() {
    assertThatThrownBy(() -> Money.of(new BigDecimal("1.00"), Currency.getInstance("USD")))
        .isInstanceOf(InvalidMoneyException.class)
        .hasMessageContaining("USD");
  }

  @Test
  void null_amount_is_rejected() {
    assertThatThrownBy(() -> Money.of((String) null)).isInstanceOf(InvalidMoneyException.class);
  }

  @Test
  void a_malformed_amount_is_rejected() {
    assertThatThrownBy(() -> Money.of("12abc")).isInstanceOf(InvalidMoneyException.class);
  }

  @Test
  void plus_and_minus_are_exact() {
    var balance = Money.of("0.10").plus(Money.of("0.20"));

    assertThat(balance).isEqualTo(Money.of("0.30"));
    assertThat(balance.minus(Money.of("0.30"))).isEqualTo(Money.zero());
  }

  @Test
  void minus_can_go_below_zero_and_says_so() {
    var result = Money.of("5.00").minus(Money.of("7.50"));

    assertThat(result.isNegative()).isTrue();
    assertThat(result.amount()).isEqualByComparingTo("-2.50");
  }

  @Test
  void comparisons_follow_the_amount() {
    assertThat(Money.of("10").isGreaterThan(Money.of("9.99"))).isTrue();
    assertThat(Money.of("10").isLessThan(Money.of("10.00"))).isFalse();
    assertThat(Money.of("0.01").isPositive()).isTrue();
    assertThat(Money.zero().isPositive()).isFalse();
  }

  @Test
  void prints_amount_then_currency() {
    assertThat(Money.of("150000")).hasToString("150000.00 IDR");
  }
}
