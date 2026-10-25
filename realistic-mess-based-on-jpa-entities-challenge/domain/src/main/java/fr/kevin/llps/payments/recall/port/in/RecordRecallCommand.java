package fr.kevin.llps.payments.recall.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * An ingested recall. Identifiers are raw text: they are validated when the recall is recorded.
 */
public record RecordRecallCommand(String id,
                                  String recallReference,
                                  String recallType,
                                  String originalPaymentId,
                                  String originalPaymentType,
                                  String reasonCode,
                                  BigDecimal amount,
                                  String currency,
                                  LocalDateTime initiatedAt,
                                  LocalDate deadline,
                                  String status) {
}
