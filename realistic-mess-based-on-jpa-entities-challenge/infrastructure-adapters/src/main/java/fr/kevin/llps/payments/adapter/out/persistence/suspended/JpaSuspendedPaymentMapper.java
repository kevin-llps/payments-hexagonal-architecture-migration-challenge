package fr.kevin.llps.payments.adapter.out.persistence.suspended;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.suspended.SuspendedPayment;

/**
 * Maps the suspended-payment aggregate to and from its unchanged JPA entity. Wall-clock times are converted with
 * {@link Timestamp#valueOf(LocalDateTime)} and {@link Timestamp#toLocalDateTime()}, as the legacy code did.
 */
final class JpaSuspendedPaymentMapper {

    SuspendedPayment toDomain(fr.kevin.llps.payments.entity.SuspendedPayment entity) {
        return SuspendedPayment.builder(PaymentId.of(entity.id))
                .money(Money.of(entity.amount, entity.currency))
                .debtorIban(entity.debtorIban)
                .debtorName(entity.debtorName)
                .creditorIban(entity.creditorIban)
                .creditorName(entity.creditorName)
                .holdReason(entity.holdReason)
                .complianceCaseRef(entity.complianceCaseRef)
                .reviewStatus(entity.reviewStatus)
                .heldAt(toLocalDateTime(entity.heldAt))
                .releasedAt(toLocalDateTime(entity.releasedAt))
                .createdAt(toLocalDateTime(entity.createdAt))
                .updatedAt(toLocalDateTime(entity.updatedAt))
                .build();
    }

    fr.kevin.llps.payments.entity.SuspendedPayment toEntity(SuspendedPayment payment) {
        fr.kevin.llps.payments.entity.SuspendedPayment entity = new fr.kevin.llps.payments.entity.SuspendedPayment();
        entity.id = payment.id().value();
        entity.amount = payment.money().amount().orElse(null);
        entity.currency = payment.money().currency().orElse(null);
        entity.debtorIban = payment.debtorIban().orElse(null);
        entity.debtorName = payment.debtorName().orElse(null);
        entity.creditorIban = payment.creditorIban().orElse(null);
        entity.creditorName = payment.creditorName().orElse(null);
        entity.holdReason = payment.holdReason().orElse(null);
        entity.complianceCaseRef = payment.complianceCaseRef().orElse(null);
        entity.reviewStatus = payment.reviewStatus().orElse(null);
        entity.heldAt = payment.heldAt().map(Timestamp::valueOf).orElse(null);
        entity.releasedAt = payment.releasedAt().map(Timestamp::valueOf).orElse(null);
        entity.createdAt = payment.createdAt().map(Timestamp::valueOf).orElse(null);
        entity.updatedAt = payment.updatedAt().map(Timestamp::valueOf).orElse(null);
        return entity;
    }

    Timestamp toTimestamp(LocalDateTime value) {
        return Timestamp.valueOf(value);
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
