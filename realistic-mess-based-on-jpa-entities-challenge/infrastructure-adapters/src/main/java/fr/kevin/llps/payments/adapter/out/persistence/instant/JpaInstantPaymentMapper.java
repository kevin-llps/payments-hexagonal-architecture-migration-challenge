package fr.kevin.llps.payments.adapter.out.persistence.instant;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import fr.kevin.llps.payments.domain.instant.InstantPayment;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * Maps the instant-payment aggregate to and from its unchanged JPA entity, converting wall-clock times with
 * {@code Timestamp.valueOf} / {@code toLocalDateTime} as the legacy code did.
 */
final class JpaInstantPaymentMapper {

    InstantPayment toDomain(fr.kevin.llps.payments.entity.InstantPayment entity) {
        return InstantPayment.builder(PaymentId.of(entity.id))
                .endToEndId(entity.endToEndId)
                .uetr(entity.uetr)
                .money(Money.of(entity.amount, entity.currency))
                .debtorIban(entity.debtorIban)
                .debtorName(entity.debtorName)
                .creditorIban(entity.creditorIban)
                .creditorName(entity.creditorName)
                .executionDatetime(toLocalDateTime(entity.executionDatetime))
                .status(entity.status)
                .scheme(entity.scheme)
                .createdAt(toLocalDateTime(entity.createdAt))
                .updatedAt(toLocalDateTime(entity.updatedAt))
                .build();
    }

    fr.kevin.llps.payments.entity.InstantPayment toEntity(InstantPayment payment) {
        fr.kevin.llps.payments.entity.InstantPayment entity = new fr.kevin.llps.payments.entity.InstantPayment();
        entity.id = payment.id().value();
        entity.endToEndId = payment.endToEndId().orElse(null);
        entity.uetr = payment.uetr().orElse(null);
        entity.amount = payment.money().amount().orElse(null);
        entity.currency = payment.money().currency().orElse(null);
        entity.debtorIban = payment.debtorIban().orElse(null);
        entity.debtorName = payment.debtorName().orElse(null);
        entity.creditorIban = payment.creditorIban().orElse(null);
        entity.creditorName = payment.creditorName().orElse(null);
        entity.executionDatetime = payment.executionDatetime().map(Timestamp::valueOf).orElse(null);
        entity.status = payment.status().orElse(null);
        entity.scheme = payment.scheme().orElse(null);
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
