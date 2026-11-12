package fr.kevin.llps.payments.domain.recall;

import java.util.Objects;
import java.util.Optional;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * Reference to the recalled payment; neither its existence nor its type is checked (BR-RCL-07).
 */
public final class OriginalPaymentReference {

    private final PaymentId paymentId;
    private final String paymentType;

    private OriginalPaymentReference(PaymentId paymentId, String paymentType) {
        this.paymentId = paymentId;
        this.paymentType = paymentType;
    }

    public static OriginalPaymentReference of(PaymentId paymentId, String paymentType) {
        return new OriginalPaymentReference(paymentId, paymentType);
    }

    public Optional<PaymentId> paymentId() {
        return Optional.ofNullable(paymentId);
    }

    public Optional<String> paymentType() {
        return Optional.ofNullable(paymentType);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OriginalPaymentReference reference
                && Objects.equals(paymentId, reference.paymentId)
                && Objects.equals(paymentType, reference.paymentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, paymentType);
    }

    @Override
    public String toString() {
        return "OriginalPaymentReference[paymentId=" + paymentId + ", paymentType=" + paymentType + "]";
    }
}
