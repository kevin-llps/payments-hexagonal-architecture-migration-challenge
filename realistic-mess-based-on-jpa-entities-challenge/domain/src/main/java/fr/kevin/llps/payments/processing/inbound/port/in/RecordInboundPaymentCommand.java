package fr.kevin.llps.payments.processing.inbound.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An ingested inbound payment. The identifier is raw text: it is validated when the payment is recorded.
 */
public record RecordInboundPaymentCommand(String id,
                                          String creditTransferRef,
                                          BigDecimal amount,
                                          String currency,
                                          String creditorIban,
                                          String creditorName,
                                          String originBic,
                                          LocalDate valueDate,
                                          LocalDate settlementDate,
                                          String status) {
}
