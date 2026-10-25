package fr.kevin.llps.payments.recall;

import fr.kevin.llps.payments.shared.domain.PaymentId;

/**
 * Reference to the payment a recall is about. The payment itself is never looked up.
 */
public record OriginalPaymentRef(PaymentId id, String type) {
}
