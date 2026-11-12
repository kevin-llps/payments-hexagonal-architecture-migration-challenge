package fr.kevin.llps.payments.adapter.out.persistence.inbound;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import fr.kevin.llps.payments.domain.inbound.InboundPayment;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * Maps the inbound-payment aggregate to and from its unchanged JPA entity ({@code Date} / {@code Timestamp} converted
 * with {@code valueOf} and {@code toLocalDate} / {@code toLocalDateTime}).
 */
final class JpaInboundPaymentMapper {

    InboundPayment toDomain(fr.kevin.llps.payments.entity.InboundPayment entity) {
        return InboundPayment.builder(PaymentId.of(entity.id))
                .creditTransferRef(entity.creditTransferRef)
                .money(Money.of(entity.amount, entity.currency))
                .creditorIban(entity.creditorIban)
                .creditorName(entity.creditorName)
                .originBic(entity.originBic)
                .valueDate(toLocalDate(entity.valueDate))
                .settlementDate(toLocalDate(entity.settlementDate))
                .status(entity.status)
                .createdAt(toLocalDateTime(entity.createdAt))
                .updatedAt(toLocalDateTime(entity.updatedAt))
                .build();
    }

    fr.kevin.llps.payments.entity.InboundPayment toEntity(InboundPayment payment) {
        fr.kevin.llps.payments.entity.InboundPayment entity = new fr.kevin.llps.payments.entity.InboundPayment();
        entity.id = payment.id().value();
        entity.creditTransferRef = payment.creditTransferRef().orElse(null);
        entity.amount = payment.money().amount().orElse(null);
        entity.currency = payment.money().currency().orElse(null);
        entity.creditorIban = payment.creditorIban().orElse(null);
        entity.creditorName = payment.creditorName().orElse(null);
        entity.originBic = payment.originBic().orElse(null);
        entity.valueDate = payment.valueDate().map(Date::valueOf).orElse(null);
        entity.settlementDate = payment.settlementDate().map(Date::valueOf).orElse(null);
        entity.status = payment.status().orElse(null);
        entity.createdAt = payment.createdAt().map(Timestamp::valueOf).orElse(null);
        entity.updatedAt = payment.updatedAt().map(Timestamp::valueOf).orElse(null);
        return entity;
    }

    Date toDate(LocalDate value) {
        return Date.valueOf(value);
    }

    private static LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
