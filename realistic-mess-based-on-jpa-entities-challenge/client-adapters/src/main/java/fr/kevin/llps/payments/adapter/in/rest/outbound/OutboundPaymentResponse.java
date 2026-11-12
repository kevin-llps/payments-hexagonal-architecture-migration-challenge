package fr.kevin.llps.payments.adapter.in.rest.outbound;

import java.math.BigDecimal;

/**
 * JSON shape of an outbound search row, identical to the legacy DTO (same names, same order, nulls kept).
 */
public record OutboundPaymentResponse(String id, String instructionId, BigDecimal amount, String currency,
        String debtorIban, String debtorName, String destinationBic, String requestedExecutionDate,
        String chargesType, String status, BigDecimal estimatedFee) {
}
