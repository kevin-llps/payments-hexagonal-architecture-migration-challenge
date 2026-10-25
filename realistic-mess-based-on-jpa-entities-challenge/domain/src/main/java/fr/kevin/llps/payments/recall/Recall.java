package fr.kevin.llps.payments.recall;

import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.DomainInvariantViolationException;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A recall request on a previously executed payment. Amount and currency are optional and independent.
 */
public record Recall(PaymentId id,
                     String recallReference,
                     String recallType,
                     OriginalPaymentRef originalPayment,
                     String reasonCode,
                     BigDecimal amount,
                     String currency,
                     LocalDateTime initiatedAt,
                     LocalDate deadline,
                     String status,
                     AuditTimestamps audit) {

    /**
     * Records an ingested recall; it must reference its original payment.
     */
    public static Recall record(PaymentId id,
                                String recallReference,
                                String recallType,
                                PaymentId originalPaymentId,
                                String originalPaymentType,
                                String reasonCode,
                                BigDecimal amount,
                                String currency,
                                LocalDateTime initiatedAt,
                                LocalDate deadline,
                                String status,
                                AuditTimestamps audit) {
        if (originalPaymentId == null) {
            throw new DomainInvariantViolationException("A recall must reference its original payment");
        }
        return new Recall(id, recallReference, recallType, new OriginalPaymentRef(originalPaymentId, originalPaymentType),
                reasonCode, amount, currency, initiatedAt, deadline, status, audit);
    }
}
