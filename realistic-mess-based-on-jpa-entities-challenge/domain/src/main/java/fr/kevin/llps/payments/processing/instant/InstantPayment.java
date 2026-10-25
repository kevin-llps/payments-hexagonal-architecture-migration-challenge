package fr.kevin.llps.payments.processing.instant;

import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.time.LocalDateTime;

/**
 * An executed SEPA instant credit transfer.
 */
public record InstantPayment(PaymentId id,
                             String endToEndId,
                             String uetr,
                             Money amount,
                             String debtorIban,
                             String debtorName,
                             String creditorIban,
                             String creditorName,
                             LocalDateTime executionDatetime,
                             String status,
                             String scheme,
                             AuditTimestamps audit) {

    private static final String DEFAULT_SCHEME = "SCT INST";

    /**
     * Records an ingested instant payment; a payment without scheme is an {@code SCT INST} one.
     */
    public static InstantPayment record(PaymentId id,
                                        String endToEndId,
                                        String uetr,
                                        Money amount,
                                        String debtorIban,
                                        String debtorName,
                                        String creditorIban,
                                        String creditorName,
                                        LocalDateTime executionDatetime,
                                        String status,
                                        String scheme,
                                        AuditTimestamps audit) {
        return new InstantPayment(id, endToEndId, uetr, amount, debtorIban, debtorName, creditorIban, creditorName,
                executionDatetime, status, scheme == null ? DEFAULT_SCHEME : scheme, audit);
    }
}
