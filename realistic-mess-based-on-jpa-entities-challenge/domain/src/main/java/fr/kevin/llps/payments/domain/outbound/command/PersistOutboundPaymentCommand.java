package fr.kevin.llps.payments.domain.outbound.command;

import java.math.BigDecimal;
import java.time.LocalDate;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * An outbound-payment event as parsed by the driving adapter; text values are kept raw ({@code ""}, {@code "null"}).
 */
public record PersistOutboundPaymentCommand(PaymentId id, String instructionId, BigDecimal amount, String currency,
        String debtorIban, String debtorName, String destinationBic, LocalDate requestedExecutionDate,
        String chargesType, String status) {
}
