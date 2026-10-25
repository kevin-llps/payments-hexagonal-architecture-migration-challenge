package fr.kevin.llps.payments.recall.port.in;

/**
 * Recall search filters as received from the caller.
 */
public record RecallSearchQuery(String recallType,
                                String originalPaymentId,
                                String originalPaymentType,
                                String reasonCode,
                                String status,
                                String initiatedFrom,
                                String initiatedTo,
                                Integer limit) {
}
