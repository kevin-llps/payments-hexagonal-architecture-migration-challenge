package fr.kevin.llps.payments.domain.outbound;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * An outbound payment with the fee estimated by the policy of the use case that returned it.
 */
public final class OutboundPaymentWithFee {

    private final OutboundPayment payment;
    private final BigDecimal estimatedFee;

    public OutboundPaymentWithFee(OutboundPayment payment, BigDecimal estimatedFee) {
        this.payment = Objects.requireNonNull(payment, "payment");
        this.estimatedFee = Objects.requireNonNull(estimatedFee, "estimatedFee");
    }

    public OutboundPayment payment() {
        return payment;
    }

    public BigDecimal estimatedFee() {
        return estimatedFee;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OutboundPaymentWithFee withFee
                && payment.equals(withFee.payment)
                && estimatedFee.equals(withFee.estimatedFee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payment, estimatedFee);
    }

    @Override
    public String toString() {
        return "OutboundPaymentWithFee[payment=" + payment + ", estimatedFee=" + estimatedFee + "]";
    }
}
