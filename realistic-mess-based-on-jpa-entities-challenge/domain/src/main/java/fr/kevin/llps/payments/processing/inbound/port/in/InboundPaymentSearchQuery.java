package fr.kevin.llps.payments.processing.inbound.port.in;

/**
 * Inbound payment search filters as received from the caller.
 */
public record InboundPaymentSearchQuery(String creditorIban,
                                        String originBic,
                                        String status,
                                        String currency,
                                        String valueDateFrom,
                                        String valueDateTo,
                                        Integer limit) {
}
