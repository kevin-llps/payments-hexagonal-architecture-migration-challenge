package fr.kevin.llps.payments.adapter.in.rest.recall;

import java.math.BigDecimal;

/**
 * JSON shape of a recall, identical to the legacy DTO (same names, same order, nulls kept).
 */
public record RecallResponse(String id, String recallReference, String recallType, String originalPaymentId,
        String originalPaymentType, String reasonCode, BigDecimal amount, String currency, String initiatedAt,
        String deadline, String status) {
}
