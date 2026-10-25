package fr.kevin.llps.payments.compliancehold.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * An ingested suspended payment. The identifier is raw text: it is validated when the payment is recorded.
 */
public record RecordSuspendedPaymentCommand(String id,
                                            BigDecimal amount,
                                            String currency,
                                            String debtorIban,
                                            String debtorName,
                                            String creditorIban,
                                            String creditorName,
                                            String holdReason,
                                            String complianceCaseRef,
                                            String reviewStatus,
                                            LocalDateTime heldAt,
                                            LocalDateTime releasedAt) {
}
