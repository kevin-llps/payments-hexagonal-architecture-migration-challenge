package fr.kevin.llps.payments.processing.inbound;

import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A credit transfer received from another bank.
 */
public record InboundPayment(PaymentId id,
                             String creditTransferRef,
                             Money amount,
                             String creditorIban,
                             String creditorName,
                             String originBic,
                             LocalDate valueDate,
                             LocalDate settlementDate,
                             String status,
                             AuditTimestamps audit) {

    /**
     * @return days elapsed from the value date to {@code today} (negative for a future value date),
     * or null without value date
     */
    public Long ageInDays(LocalDate today) {
        return valueDate == null ? null : ChronoUnit.DAYS.between(valueDate, today);
    }
}
