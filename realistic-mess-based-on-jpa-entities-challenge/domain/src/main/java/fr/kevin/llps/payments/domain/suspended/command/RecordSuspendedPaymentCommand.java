package fr.kevin.llps.payments.domain.suspended.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * A suspended-payment event as parsed by the driving adapter; text values are kept raw ({@code ""}, {@code "null"}).
 */
public record RecordSuspendedPaymentCommand(PaymentId id, BigDecimal amount, String currency, String debtorIban,
        String debtorName, String creditorIban, String creditorName, String holdReason, String complianceCaseRef,
        String reviewStatus, LocalDateTime heldAt, LocalDateTime releasedAt) {
}
