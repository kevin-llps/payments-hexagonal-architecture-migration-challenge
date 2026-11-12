package fr.kevin.llps.payments.domain.instant.query;

/**
 * Raw instant-payment search request values, validated by the use case.
 */
public record InstantPaymentSearchQuery(String endToEndId, String debtorIban, String creditorIban, String status,
        String currency, String executionFrom, String executionTo, Integer limit) {
}
