package fr.kevin.llps.payments.domain.inbound;

import java.util.Objects;
import java.util.Optional;

/**
 * An inbound payment with its age in days, as returned by the search and details use cases (BR-INB-07, BR-INB-08).
 */
public final class InboundPaymentView {

    private final InboundPayment payment;
    private final Long ageInDays;

    public InboundPaymentView(InboundPayment payment, Long ageInDays) {
        this.payment = Objects.requireNonNull(payment, "payment");
        this.ageInDays = ageInDays;
    }

    public InboundPayment payment() {
        return payment;
    }

    public Optional<Long> ageInDays() {
        return Optional.ofNullable(ageInDays);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof InboundPaymentView view
                && payment.equals(view.payment)
                && Objects.equals(ageInDays, view.ageInDays);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payment, ageInDays);
    }

    @Override
    public String toString() {
        return "InboundPaymentView[payment=" + payment + ", ageInDays=" + ageInDays + "]";
    }
}
