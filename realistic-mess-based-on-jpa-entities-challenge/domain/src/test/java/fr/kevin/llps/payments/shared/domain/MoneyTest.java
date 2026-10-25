package fr.kevin.llps.payments.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @ParameterizedTest(name = "[{index}] currency=\"{0}\"")
    @ValueSource(strings = {"EUR", "", "null"})
    void of_should_keep_amount_and_currency_as_received(String currency) {
        // GIVEN
        BigDecimal amount = new BigDecimal("1500.00");

        // WHEN
        Money money = Money.of(amount, currency);

        // THEN
        assertThat(money).isEqualTo(new Money(new BigDecimal("1500.00"), currency));
    }

    @Test
    void of_should_throw_domain_invariant_violation_when_amount_is_null() {
        // GIVEN a stored payment without amount (unreachable in production, D-01)

        // WHEN / THEN
        assertThatThrownBy(() -> Money.of(null, "EUR"))
                .isInstanceOf(DomainInvariantViolationException.class)
                .hasMessage("Money amount is mandatory");
    }
}
