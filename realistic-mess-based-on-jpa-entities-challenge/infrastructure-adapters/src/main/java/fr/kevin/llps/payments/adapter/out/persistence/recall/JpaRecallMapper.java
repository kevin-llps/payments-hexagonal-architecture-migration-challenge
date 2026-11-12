package fr.kevin.llps.payments.adapter.out.persistence.recall;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import fr.kevin.llps.payments.domain.recall.OriginalPaymentReference;
import fr.kevin.llps.payments.domain.recall.Recall;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * Maps the recall aggregate to and from its unchanged JPA entity, converting wall-clock times and dates with
 * {@code Timestamp.valueOf} / {@code Date.valueOf} as the legacy code did.
 */
final class JpaRecallMapper {

    Recall toDomain(fr.kevin.llps.payments.entity.Recall entity) {
        return Recall.builder(PaymentId.of(entity.id))
                .recallReference(entity.recallReference)
                .recallType(entity.recallType)
                .originalPayment(OriginalPaymentReference.of(toPaymentId(entity.originalPaymentId), entity.originalPaymentType))
                .reasonCode(entity.reasonCode)
                .money(Money.of(entity.amount, entity.currency))
                .initiatedAt(toLocalDateTime(entity.initiatedAt))
                .deadline(entity.deadline == null ? null : entity.deadline.toLocalDate())
                .status(entity.status)
                .createdAt(toLocalDateTime(entity.createdAt))
                .updatedAt(toLocalDateTime(entity.updatedAt))
                .build();
    }

    fr.kevin.llps.payments.entity.Recall toEntity(Recall recall) {
        fr.kevin.llps.payments.entity.Recall entity = new fr.kevin.llps.payments.entity.Recall();
        entity.id = recall.id().value();
        entity.recallReference = recall.recallReference().orElse(null);
        entity.recallType = recall.recallType().orElse(null);
        entity.originalPaymentId = recall.originalPayment().paymentId().map(PaymentId::value).orElse(null);
        entity.originalPaymentType = recall.originalPayment().paymentType().orElse(null);
        entity.reasonCode = recall.reasonCode().orElse(null);
        entity.amount = recall.money().amount().orElse(null);
        entity.currency = recall.money().currency().orElse(null);
        entity.initiatedAt = recall.initiatedAt().map(Timestamp::valueOf).orElse(null);
        entity.deadline = recall.deadline().map(Date::valueOf).orElse(null);
        entity.status = recall.status().orElse(null);
        entity.createdAt = recall.createdAt().map(Timestamp::valueOf).orElse(null);
        entity.updatedAt = recall.updatedAt().map(Timestamp::valueOf).orElse(null);
        return entity;
    }

    Timestamp toTimestamp(LocalDateTime value) {
        return Timestamp.valueOf(value);
    }

    private static PaymentId toPaymentId(UUID value) {
        return value == null ? null : PaymentId.of(value);
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
