package fr.kevin.llps.payments.domain.outbound.query;

/**
 * Raw outbound-payment search request values, validated by the use case.
 */
public record OutboundPaymentSearchQuery(String debtorIban, String destinationBic, String chargesType, String status,
        String executionFrom, String executionTo, Integer limit) {
}
