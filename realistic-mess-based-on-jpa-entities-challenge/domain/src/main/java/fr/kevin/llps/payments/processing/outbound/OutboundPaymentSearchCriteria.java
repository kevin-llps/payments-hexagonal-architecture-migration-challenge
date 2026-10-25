package fr.kevin.llps.payments.processing.outbound;

import fr.kevin.llps.payments.shared.domain.LenientDate;
import fr.kevin.llps.payments.shared.domain.MalformedValueException;
import fr.kevin.llps.payments.shared.domain.SearchLimit;

import java.time.LocalDate;

/**
 * Normalized outbound payment search: the destination BIC value matches any part of the stored BIC as stored (the
 * value is upper-cased, the stored BIC is not), other values are exact matches, bounds are inclusive, results are
 * ordered by requested execution date, most recent first.
 */
public record OutboundPaymentSearchCriteria(String debtorIban,
                                            String destinationBicContains,
                                            String chargesType,
                                            String status,
                                            LocalDate executionFrom,
                                            LocalDate executionTo,
                                            SearchLimit limit) {

    public static OutboundPaymentSearchCriteria from(String debtorIban,
                                                     String destinationBic,
                                                     String chargesType,
                                                     String status,
                                                     String executionFrom,
                                                     String executionTo,
                                                     Integer requestedLimit) {
        return new OutboundPaymentSearchCriteria(
                isBlank(debtorIban) ? null : debtorIban.trim(),
                upperCased(destinationBic),
                upperCased(chargesType),
                upperCased(status),
                executionBound(executionFrom),
                executionBound(executionTo),
                SearchLimit.capped(requestedLimit));
    }

    private static LocalDate executionBound(String value) {
        if (isBlank(value)) {
            return null;
        }
        return LenientDate.parse(value.trim())
                .orElseThrow(() -> new MalformedValueException("Invalid execution date: " + value, null));
    }

    private static String upperCased(String value) {
        return isBlank(value) ? null : value.trim().toUpperCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
