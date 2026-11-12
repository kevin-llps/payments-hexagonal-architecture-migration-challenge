package fr.kevin.llps.payments.adapter.out.persistence.outbound;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import fr.kevin.llps.payments.domain.outbound.OutboundPayment;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * Maps the outbound-payment aggregate to and from its unchanged JPA entity ({@code Date} / {@code Timestamp}
 * converted with {@code valueOf} and {@code toLocalDate} / {@code toLocalDateTime}).
 */
final class JpaOutboundPaymentMapper {

    OutboundPayment toDomain(fr.kevin.llps.payments.entity.OutboundPayment entity) {
        return OutboundPayment.builder(PaymentId.of(entity.id))
                .instructionId(entity.instructionId)
                .money(Money.of(entity.amount, entity.currency))
                .debtorIban(entity.debtorIban)
                .debtorName(entity.debtorName)
                .destinationBic(entity.destinationBic)
                .requestedExecutionDate(entity.requestedExecutionDate == null ? null : entity.requestedExecutionDate.toLocalDate())
                .chargesType(entity.chargesType)
                .status(entity.status)
                .createdAt(toLocalDateTime(entity.createdAt))
                .updatedAt(toLocalDateTime(entity.updatedAt))
                .build();
    }

    fr.kevin.llps.payments.entity.OutboundPayment toEntity(OutboundPayment payment) {
        fr.kevin.llps.payments.entity.OutboundPayment entity = new fr.kevin.llps.payments.entity.OutboundPayment();
        entity.id = payment.id().value();
        entity.instructionId = payment.instructionId().orElse(null);
        entity.amount = payment.money().amount().orElse(null);
        entity.currency = payment.money().currency().orElse(null);
        entity.debtorIban = payment.debtorIban().orElse(null);
        entity.debtorName = payment.debtorName().orElse(null);
        entity.destinationBic = payment.destinationBic().orElse(null);
        entity.requestedExecutionDate = payment.requestedExecutionDate().map(Date::valueOf).orElse(null);
        entity.chargesType = payment.chargesType().orElse(null);
        entity.status = payment.status().orElse(null);
        entity.createdAt = payment.createdAt().map(Timestamp::valueOf).orElse(null);
        entity.updatedAt = payment.updatedAt().map(Timestamp::valueOf).orElse(null);
        return entity;
    }

    Date toDate(LocalDate value) {
        return Date.valueOf(value);
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
