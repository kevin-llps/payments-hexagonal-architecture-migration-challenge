package fr.kevin.llps.payments.adapter.in.rest.inbound;

import java.math.BigDecimal;

/**
 * JSON shape of an inbound payment, identical to the legacy DTO (same names, same order, nulls kept).
 */
public record InboundPaymentResponse(String id, String creditTransferRef, BigDecimal amount, String currency,
        String creditorIban, String creditorName, String originBic, String valueDate, String settlementDate,
        String status, Long ageInDays) {
}
