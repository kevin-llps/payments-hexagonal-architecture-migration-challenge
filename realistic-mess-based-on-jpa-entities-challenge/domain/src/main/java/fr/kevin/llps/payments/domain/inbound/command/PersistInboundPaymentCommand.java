package fr.kevin.llps.payments.domain.inbound.command;

import java.math.BigDecimal;
import java.time.LocalDate;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * An inbound-payment event as parsed by the driving adapter; text values are kept raw ({@code ""}, {@code "null"}).
 */
public record PersistInboundPaymentCommand(PaymentId id, String creditTransferRef, BigDecimal amount, String currency,
        String creditorIban, String creditorName, String originBic, LocalDate valueDate, LocalDate settlementDate,
        String status) {
}
