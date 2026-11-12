package fr.kevin.llps.payments.domain.recall.query;

/**
 * Raw recall search request values; {@code limit} is accepted but ignored (BR-RCL-05).
 */
public record RecallSearchQuery(String recallType, String originalPaymentId, String originalPaymentType,
        String reasonCode, String status, String initiatedFrom, String initiatedTo, Integer limit) {
}
