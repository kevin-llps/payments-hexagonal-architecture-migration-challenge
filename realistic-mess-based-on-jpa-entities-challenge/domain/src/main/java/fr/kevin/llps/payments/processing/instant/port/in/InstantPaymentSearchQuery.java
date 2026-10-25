package fr.kevin.llps.payments.processing.instant.port.in;

/**
 * Instant payment search filters as received from the caller.
 */
public record InstantPaymentSearchQuery(String endToEndId,
                                        String debtorIban,
                                        String creditorIban,
                                        String status,
                                        String currency,
                                        String executionFrom,
                                        String executionTo,
                                        Integer limit) {
}
