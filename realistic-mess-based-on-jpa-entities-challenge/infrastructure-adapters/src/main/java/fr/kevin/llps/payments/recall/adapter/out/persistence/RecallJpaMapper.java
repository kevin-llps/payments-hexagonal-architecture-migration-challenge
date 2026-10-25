package fr.kevin.llps.payments.recall.adapter.out.persistence;

import fr.kevin.llps.payments.recall.adapter.out.persistence.entity.Recall;
import fr.kevin.llps.payments.recall.OriginalPaymentRef;
import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity / aggregate conversion. Reconstitution is lossless: nullable columns stay null.
 */
final class RecallJpaMapper {

    private RecallJpaMapper() {
    }

    static fr.kevin.llps.payments.recall.Recall toDomain(Recall entity) {
        return new fr.kevin.llps.payments.recall.Recall(
                new PaymentId(entity.id),
                entity.recallReference,
                entity.recallType,
                new OriginalPaymentRef(
                        entity.originalPaymentId == null ? null : new PaymentId(entity.originalPaymentId),
                        entity.originalPaymentType),
                entity.reasonCode,
                entity.amount,
                entity.currency,
                toLocalDateTime(entity.initiatedAt),
                entity.deadline == null ? null : entity.deadline.toLocalDate(),
                entity.status,
                new AuditTimestamps(toLocalDateTime(entity.createdAt), toLocalDateTime(entity.updatedAt)));
    }

    static Recall toEntity(fr.kevin.llps.payments.recall.Recall recall) {
        Recall entity = new Recall();
        entity.id = recall.id().value();
        entity.recallReference = recall.recallReference();
        entity.recallType = recall.recallType();
        PaymentId originalPaymentId = recall.originalPayment().id();
        entity.originalPaymentId = originalPaymentId == null ? null : originalPaymentId.value();
        entity.originalPaymentType = recall.originalPayment().type();
        entity.reasonCode = recall.reasonCode();
        entity.amount = recall.amount();
        entity.currency = recall.currency();
        entity.initiatedAt = toTimestamp(recall.initiatedAt());
        entity.deadline = toDate(recall.deadline());
        entity.status = recall.status();
        if (recall.audit() != null) {
            entity.createdAt = toTimestamp(recall.audit().createdAt());
            entity.updatedAt = toTimestamp(recall.audit().updatedAt());
        }
        return entity;
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static Date toDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }
}
