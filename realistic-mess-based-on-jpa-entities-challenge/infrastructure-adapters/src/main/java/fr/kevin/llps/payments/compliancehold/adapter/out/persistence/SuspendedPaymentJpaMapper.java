package fr.kevin.llps.payments.compliancehold.adapter.out.persistence;

import fr.kevin.llps.payments.compliancehold.adapter.out.persistence.entity.SuspendedPayment;
import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Entity / aggregate conversion. A row without amount cannot be reconstituted (D-01; the column is NOT NULL).
 */
final class SuspendedPaymentJpaMapper {

    private SuspendedPaymentJpaMapper() {
    }

    static fr.kevin.llps.payments.compliancehold.SuspendedPayment toDomain(SuspendedPayment entity) {
        return new fr.kevin.llps.payments.compliancehold.SuspendedPayment(
                new PaymentId(entity.id),
                Money.of(entity.amount, entity.currency),
                entity.debtorIban,
                entity.debtorName,
                entity.creditorIban,
                entity.creditorName,
                entity.holdReason,
                entity.complianceCaseRef,
                entity.reviewStatus,
                toLocalDateTime(entity.heldAt),
                toLocalDateTime(entity.releasedAt),
                new AuditTimestamps(toLocalDateTime(entity.createdAt), toLocalDateTime(entity.updatedAt)));
    }

    static SuspendedPayment toEntity(fr.kevin.llps.payments.compliancehold.SuspendedPayment payment) {
        SuspendedPayment entity = new SuspendedPayment();
        entity.id = payment.id().value();
        entity.amount = payment.amount().amount();
        entity.currency = payment.amount().currency();
        entity.debtorIban = payment.debtorIban();
        entity.debtorName = payment.debtorName();
        entity.creditorIban = payment.creditorIban();
        entity.creditorName = payment.creditorName();
        entity.holdReason = payment.holdReason();
        entity.complianceCaseRef = payment.complianceCaseRef();
        entity.reviewStatus = payment.reviewStatus();
        entity.heldAt = toTimestamp(payment.heldAt());
        entity.releasedAt = toTimestamp(payment.releasedAt());
        if (payment.audit() != null) {
            entity.createdAt = toTimestamp(payment.audit().createdAt());
            entity.updatedAt = toTimestamp(payment.audit().updatedAt());
        }
        return entity;
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
