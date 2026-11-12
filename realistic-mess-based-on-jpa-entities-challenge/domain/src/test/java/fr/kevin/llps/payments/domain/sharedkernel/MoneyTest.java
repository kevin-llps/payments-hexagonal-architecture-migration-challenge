package fr.kevin.llps.payments.domain.sharedkernel;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void should_keep_amount_scale_and_currency_unchanged() {
        // GIVEN
        BigDecimal amount = new BigDecimal("780.00");

        // WHEN
        Money actual = Money.of(amount, "EUR");

        // THEN
        assertThat(actual)
                .returns(Optional.of(new BigDecimal("780.00")), Money::amount)
                .returns(Optional.of("EUR"), Money::currency);
    }

    @Test
    void should_expose_empty_amount_and_currency_when_both_are_absent() {
        // GIVEN
        BigDecimal noAmount = null;

        // WHEN
        Money actual = Money.of(noAmount, null);

        // THEN
        assertThat(actual)
                .returns(Optional.empty(), Money::amount)
                .returns(Optional.empty(), Money::currency);
    }
}
