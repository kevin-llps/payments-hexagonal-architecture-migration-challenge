package fr.kevin.llps.payments.processing.outbound.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An ingested outbound payment. The identifier is raw text: it is validated when the payment is recorded.
 */
public record RecordOutboundPaymentCommand(String id,
                                           String instructionId,
                                           BigDecimal amount,
                                           String currency,
                                           String debtorIban,
                                           String debtorName,
                                           String destinationBic,
                                           LocalDate requestedExecutionDate,
                                           String chargesType,
                                           String status) {
}
