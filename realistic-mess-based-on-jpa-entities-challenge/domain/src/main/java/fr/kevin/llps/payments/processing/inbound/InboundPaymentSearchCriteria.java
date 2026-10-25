package fr.kevin.llps.payments.processing.inbound;

import fr.kevin.llps.payments.shared.domain.LenientDate;
import fr.kevin.llps.payments.shared.domain.MalformedValueException;
import fr.kevin.llps.payments.shared.domain.SearchLimit;

import java.time.LocalDate;

/**
 * Normalized inbound payment search: the origin BIC matches any part of the stored BIC ignoring case, other values are
 * exact matches, bounds are inclusive, results are ordered by value date then creation date, most recent first.
 */
public record InboundPaymentSearchCriteria(String creditorIban,
                                           String originBicContains,
                                           String status,
                                           String currency,
                                           LocalDate valueDateFrom,
                                           LocalDate valueDateTo,
                                           SearchLimit limit) {

    public static InboundPaymentSearchCriteria from(String creditorIban,
                                                    String originBic,
                                                    String status,
                                                    String currency,
                                                    String valueDateFrom,
                                                    String valueDateTo,
                                                    Integer requestedLimit) {
        return new InboundPaymentSearchCriteria(
                isBlank(creditorIban) ? null : creditorIban.trim(),
                upperCased(originBic),
                upperCased(status),
                upperCased(currency),
                valueDateBound(valueDateFrom),
                valueDateBound(valueDateTo),
                SearchLimit.uncapped(requestedLimit));
    }

    private static LocalDate valueDateBound(String value) {
        if (isBlank(value)) {
            return null;
        }
        return LenientDate.parse(value.trim())
                .orElseThrow(() -> new MalformedValueException("Invalid value date: " + value, null));
    }

    private static String upperCased(String value) {
        return isBlank(value) ? null : value.trim().toUpperCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
