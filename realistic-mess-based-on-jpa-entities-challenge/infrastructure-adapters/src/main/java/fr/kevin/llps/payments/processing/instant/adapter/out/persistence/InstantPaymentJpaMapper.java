package fr.kevin.llps.payments.processing.instant.adapter.out.persistence;

import fr.kevin.llps.payments.processing.instant.adapter.out.persistence.entity.InstantPayment;
import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Entity / aggregate conversion. A row without amount cannot be reconstituted (D-01; the column is NOT NULL).
 */
final class InstantPaymentJpaMapper {

    private InstantPaymentJpaMapper() {
    }

    static fr.kevin.llps.payments.processing.instant.InstantPayment toDomain(InstantPayment entity) {
        return new fr.kevin.llps.payments.processing.instant.InstantPayment(
                new PaymentId(entity.id),
                entity.endToEndId,
                entity.uetr,
                Money.of(entity.amount, entity.currency),
                entity.debtorIban,
                entity.debtorName,
                entity.creditorIban,
                entity.creditorName,
                toLocalDateTime(entity.executionDatetime),
                entity.status,
                entity.scheme,
                new AuditTimestamps(toLocalDateTime(entity.createdAt), toLocalDateTime(entity.updatedAt)));
    }

    static InstantPayment toEntity(fr.kevin.llps.payments.processing.instant.InstantPayment payment) {
        InstantPayment entity = new InstantPayment();
        entity.id = payment.id().value();
        entity.endToEndId = payment.endToEndId();
        entity.uetr = payment.uetr();
        entity.amount = payment.amount().amount();
        entity.currency = payment.amount().currency();
        entity.debtorIban = payment.debtorIban();
        entity.debtorName = payment.debtorName();
        entity.creditorIban = payment.creditorIban();
        entity.creditorName = payment.creditorName();
        entity.executionDatetime = toTimestamp(payment.executionDatetime());
        entity.status = payment.status();
        entity.scheme = payment.scheme();
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
