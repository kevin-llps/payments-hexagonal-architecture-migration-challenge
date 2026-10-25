package fr.kevin.llps.payments.compliancehold;

import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.time.LocalDateTime;

/**
 * A payment held for compliance review.
 */
public record SuspendedPayment(PaymentId id,
                               Money amount,
                               String debtorIban,
                               String debtorName,
                               String creditorIban,
                               String creditorName,
                               String holdReason,
                               String complianceCaseRef,
                               String reviewStatus,
                               LocalDateTime heldAt,
                               LocalDateTime releasedAt,
                               AuditTimestamps audit) {
}
