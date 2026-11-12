package fr.kevin.llps.payments.domain.recall.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * A recall event as parsed by the driving adapter; text values are kept raw ({@code ""}, {@code "null"}).
 */
public record PersistRecallCommand(PaymentId id, String recallReference, String recallType,
        PaymentId originalPaymentId, String originalPaymentType, String reasonCode, BigDecimal amount,
        String currency, LocalDateTime initiatedAt, LocalDate deadline, String status) {
}
