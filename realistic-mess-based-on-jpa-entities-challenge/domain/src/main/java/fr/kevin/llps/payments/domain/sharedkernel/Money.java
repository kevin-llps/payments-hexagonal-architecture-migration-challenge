package fr.kevin.llps.payments.domain.sharedkernel;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Amount and currency carried as is: no rescaling, no mandatory part (BR-SHR-09 is enforced by the database).
 */
public final class Money {

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public Optional<BigDecimal> amount() {
        return Optional.ofNullable(amount);
    }

    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Money money
                && Objects.equals(amount, money.amount)
                && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return "Money[amount=" + amount + ", currency=" + currency + "]";
    }
}
