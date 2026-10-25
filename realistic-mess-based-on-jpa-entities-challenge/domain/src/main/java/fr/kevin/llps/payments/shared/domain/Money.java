package fr.kevin.llps.payments.shared.domain;

import java.math.BigDecimal;

/**
 * An amount and its currency. The amount is mandatory; the currency is kept as received.
 */
public record Money(BigDecimal amount, String currency) {

    public Money {
        if (amount == null) {
            throw new DomainInvariantViolationException("Money amount is mandatory");
        }
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }
}
