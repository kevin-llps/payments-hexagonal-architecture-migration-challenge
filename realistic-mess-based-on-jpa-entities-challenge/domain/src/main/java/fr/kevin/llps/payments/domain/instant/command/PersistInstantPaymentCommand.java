package fr.kevin.llps.payments.domain.instant.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * An instant-payment event as parsed by the driving adapter; text values are kept raw ({@code ""}, {@code "null"}).
 */
public record PersistInstantPaymentCommand(PaymentId id, String endToEndId, String uetr, BigDecimal amount,
        String currency, String debtorIban, String debtorName, String creditorIban, String creditorName,
        LocalDateTime executionDatetime, String status, String scheme) {
}
