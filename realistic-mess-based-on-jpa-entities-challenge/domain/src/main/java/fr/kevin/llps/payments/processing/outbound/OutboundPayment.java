package fr.kevin.llps.payments.processing.outbound;

import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.time.LocalDate;

/**
 * A credit transfer requested by a customer towards another bank.
 */
public record OutboundPayment(PaymentId id,
                              String instructionId,
                              Money amount,
                              String debtorIban,
                              String debtorName,
                              String destinationBic,
                              LocalDate requestedExecutionDate,
                              ChargesType chargesType,
                              String status,
                              AuditTimestamps audit) {
}
