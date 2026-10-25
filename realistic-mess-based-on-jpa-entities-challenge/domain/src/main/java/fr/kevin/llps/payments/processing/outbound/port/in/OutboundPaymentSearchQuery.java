package fr.kevin.llps.payments.processing.outbound.port.in;

/**
 * Outbound payment search filters as received from the caller.
 */
public record OutboundPaymentSearchQuery(String debtorIban,
                                         String destinationBic,
                                         String chargesType,
                                         String status,
                                         String executionFrom,
                                         String executionTo,
                                         Integer limit) {
}
