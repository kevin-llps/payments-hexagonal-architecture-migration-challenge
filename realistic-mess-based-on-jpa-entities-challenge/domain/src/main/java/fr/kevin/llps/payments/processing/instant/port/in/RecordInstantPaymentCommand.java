package fr.kevin.llps.payments.processing.instant.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * An ingested instant payment. The identifier is raw text: it is validated when the payment is recorded.
 * A null scheme means the message carried none.
 */
public record RecordInstantPaymentCommand(String id,
                                          String endToEndId,
                                          String uetr,
                                          BigDecimal amount,
                                          String currency,
                                          String debtorIban,
                                          String debtorName,
                                          String creditorIban,
                                          String creditorName,
                                          LocalDateTime executionDatetime,
                                          String status,
                                          String scheme) {
}
