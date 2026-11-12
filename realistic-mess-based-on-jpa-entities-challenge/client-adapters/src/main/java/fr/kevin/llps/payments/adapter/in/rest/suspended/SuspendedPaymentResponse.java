package fr.kevin.llps.payments.adapter.in.rest.suspended;

import java.math.BigDecimal;

/**
 * JSON shape of a suspended payment, identical to the legacy DTO (same names, same order, nulls kept).
 */
public record SuspendedPaymentResponse(String id, BigDecimal amount, String currency, String debtorIban,
        String debtorName, String creditorIban, String creditorName, String holdReason, String complianceCaseRef,
        String reviewStatus, String heldAt, String releasedAt) {
}
