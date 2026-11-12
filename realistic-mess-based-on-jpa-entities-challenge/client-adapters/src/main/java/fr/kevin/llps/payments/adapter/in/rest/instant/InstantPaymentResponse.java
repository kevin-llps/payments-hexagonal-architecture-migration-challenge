package fr.kevin.llps.payments.adapter.in.rest.instant;

import java.math.BigDecimal;

/**
 * JSON shape of an instant payment, identical to the legacy DTO (same names, same order, nulls kept).
 */
public record InstantPaymentResponse(String id, String endToEndId, String uetr, BigDecimal amount, String currency,
        String debtorIban, String debtorName, String creditorIban, String creditorName, String executionDatetime,
        String status, String scheme) {
}
