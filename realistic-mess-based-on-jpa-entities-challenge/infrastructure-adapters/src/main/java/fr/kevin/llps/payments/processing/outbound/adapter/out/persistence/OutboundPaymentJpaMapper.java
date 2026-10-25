package fr.kevin.llps.payments.processing.outbound.adapter.out.persistence;

import fr.kevin.llps.payments.processing.outbound.ChargesType;
import fr.kevin.llps.payments.processing.outbound.adapter.out.persistence.entity.OutboundPayment;
import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity / aggregate conversion. A row without amount cannot be reconstituted (D-01; the column is NOT NULL).
 */
final class OutboundPaymentJpaMapper {

    private OutboundPaymentJpaMapper() {
    }

    static fr.kevin.llps.payments.processing.outbound.OutboundPayment toDomain(OutboundPayment entity) {
        return new fr.kevin.llps.payments.processing.outbound.OutboundPayment(
                new PaymentId(entity.id),
                entity.instructionId,
                Money.of(entity.amount, entity.currency),
                entity.debtorIban,
                entity.debtorName,
                entity.destinationBic,
                entity.requestedExecutionDate == null ? null : entity.requestedExecutionDate.toLocalDate(),
                new ChargesType(entity.chargesType),
                entity.status,
                new AuditTimestamps(toLocalDateTime(entity.createdAt), toLocalDateTime(entity.updatedAt)));
    }

    static OutboundPayment toEntity(fr.kevin.llps.payments.processing.outbound.OutboundPayment payment) {
        OutboundPayment entity = new OutboundPayment();
        entity.id = payment.id().value();
        entity.instructionId = payment.instructionId();
        entity.amount = payment.amount().amount();
        entity.currency = payment.amount().currency();
        entity.debtorIban = payment.debtorIban();
        entity.debtorName = payment.debtorName();
        entity.destinationBic = payment.destinationBic();
        entity.requestedExecutionDate = toDate(payment.requestedExecutionDate());
        entity.chargesType = payment.chargesType().code();
        entity.status = payment.status();
        if (payment.audit() != null) {
            entity.createdAt = toTimestamp(payment.audit().createdAt());
            entity.updatedAt = toTimestamp(payment.audit().updatedAt());
        }
        return entity;
    }

    private static Date toDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
