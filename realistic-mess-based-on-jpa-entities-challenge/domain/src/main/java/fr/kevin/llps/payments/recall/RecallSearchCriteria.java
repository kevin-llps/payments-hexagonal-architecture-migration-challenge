package fr.kevin.llps.payments.recall;

import fr.kevin.llps.payments.shared.domain.LenientTimestamp;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import fr.kevin.llps.payments.shared.domain.SearchLimit;

import java.time.LocalDateTime;

/**
 * Normalized recall search: every non-null value is an exact match, bounds are inclusive,
 * results are ordered by initiation date, most recent first.
 */
public record RecallSearchCriteria(String recallType,
                                   PaymentId originalPaymentId,
                                   String originalPaymentType,
                                   String reasonCode,
                                   String status,
                                   LocalDateTime initiatedFrom,
                                   LocalDateTime initiatedTo,
                                   SearchLimit limit) {

    private static final int DATE_ONLY_LENGTH = 10;

    public static RecallSearchCriteria from(String recallType,
                                            String originalPaymentId,
                                            String originalPaymentType,
                                            String reasonCode,
                                            String status,
                                            String initiatedFrom,
                                            String initiatedTo,
                                            Integer requestedLimit) {
        return new RecallSearchCriteria(
                isBlank(recallType) ? null : RecallType.validateFilter(recallType),
                isBlank(originalPaymentId) ? null : PaymentId.parse(originalPaymentId.trim()),
                normalizedCode(originalPaymentType),
                normalizedCode(reasonCode),
                normalizedCode(status),
                initiatedBound(initiatedFrom),
                initiatedBound(initiatedTo),
                SearchLimit.fixed());
    }

    private static LocalDateTime initiatedBound(String value) {
        if (isBlank(value)) {
            return null;
        }
        String text = value.trim().replace('T', ' ');
        if (text.length() == DATE_ONLY_LENGTH) {
            text = text + " 00:00:00";
        }
        return LenientTimestamp.parse(text).orElse(null);
    }

    private static String normalizedCode(String value) {
        return isBlank(value) ? null : value.trim().toUpperCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
