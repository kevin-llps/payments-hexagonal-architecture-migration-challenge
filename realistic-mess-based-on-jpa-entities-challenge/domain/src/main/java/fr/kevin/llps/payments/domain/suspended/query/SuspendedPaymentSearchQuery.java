package fr.kevin.llps.payments.domain.suspended.query;

/**
 * Raw search request values, validated by the use case.
 */
public record SuspendedPaymentSearchQuery(String debtorIban, String holdReason, String reviewStatus, String heldFrom,
        String heldTo, Integer limit) {
}
