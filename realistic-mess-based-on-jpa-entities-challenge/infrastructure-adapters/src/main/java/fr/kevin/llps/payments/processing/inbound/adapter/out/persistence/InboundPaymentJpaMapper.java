package fr.kevin.llps.payments.processing.inbound.adapter.out.persistence;

import fr.kevin.llps.payments.processing.inbound.adapter.out.persistence.entity.InboundPayment;
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
final class InboundPaymentJpaMapper {

    private InboundPaymentJpaMapper() {
    }

    static fr.kevin.llps.payments.processing.inbound.InboundPayment toDomain(InboundPayment entity) {
        return new fr.kevin.llps.payments.processing.inbound.InboundPayment(
                new PaymentId(entity.id),
                entity.creditTransferRef,
                Money.of(entity.amount, entity.currency),
                entity.creditorIban,
                entity.creditorName,
                entity.originBic,
                toLocalDate(entity.valueDate),
                toLocalDate(entity.settlementDate),
                entity.status,
                new AuditTimestamps(toLocalDateTime(entity.createdAt), toLocalDateTime(entity.updatedAt)));
    }

    static InboundPayment toEntity(fr.kevin.llps.payments.processing.inbound.InboundPayment payment) {
        InboundPayment entity = new InboundPayment();
        entity.id = payment.id().value();
        entity.creditTransferRef = payment.creditTransferRef();
        entity.amount = payment.amount().amount();
        entity.currency = payment.amount().currency();
        entity.creditorIban = payment.creditorIban();
        entity.creditorName = payment.creditorName();
        entity.originBic = payment.originBic();
        entity.valueDate = toDate(payment.valueDate());
        entity.settlementDate = toDate(payment.settlementDate());
        entity.status = payment.status();
        if (payment.audit() != null) {
            entity.createdAt = toTimestamp(payment.audit().createdAt());
            entity.updatedAt = toTimestamp(payment.audit().updatedAt());
        }
        return entity;
    }

    private static LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
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
