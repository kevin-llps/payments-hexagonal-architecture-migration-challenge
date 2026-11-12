package fr.kevin.llps.payments.domain.inbound.query;

/**
 * Raw inbound-payment search request values, validated by the use case.
 */
public record InboundPaymentSearchQuery(String creditorIban, String originBic, String status, String currency,
        String valueDateFrom, String valueDateTo, Integer limit) {
}
