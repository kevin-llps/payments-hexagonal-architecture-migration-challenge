package fr.kevin.llps.payments.processing.instant;

import fr.kevin.llps.payments.shared.domain.IbanFilter;
import fr.kevin.llps.payments.shared.domain.InvalidSearchFilterException;
import fr.kevin.llps.payments.shared.domain.LenientTimestamp;
import fr.kevin.llps.payments.shared.domain.SearchLimit;

import java.time.LocalDateTime;

/**
 * Normalized instant payment search: IBAN values match any part of the stored IBAN ignoring case, other values are
 * exact matches, bounds are inclusive, results are ordered by execution date, most recent first.
 */
public record InstantPaymentSearchCriteria(String endToEndId,
                                           String debtorIbanContains,
                                           String creditorIbanContains,
                                           String status,
                                           String currency,
                                           LocalDateTime executionFrom,
                                           LocalDateTime executionTo,
                                           SearchLimit limit) {

    private static final int DATE_ONLY_LENGTH = 10;
    private static final String START_OF_DAY = " 00:00:00";
    private static final String END_OF_DAY = " 23:59:59";

    public static InstantPaymentSearchCriteria from(String endToEndId,
                                                    String debtorIban,
                                                    String creditorIban,
                                                    String status,
                                                    String currency,
                                                    String executionFrom,
                                                    String executionTo,
                                                    Integer requestedLimit) {
        if (!isBlank(debtorIban) && !IbanFilter.isPlausible(debtorIban)) {
            throw new InvalidSearchFilterException("Invalid debtor IBAN");
        }
        if (!isBlank(creditorIban) && !IbanFilter.isPlausible(creditorIban)) {
            throw new InvalidSearchFilterException("Invalid creditor IBAN");
        }
        return new InstantPaymentSearchCriteria(
                isBlank(endToEndId) ? null : endToEndId.trim(),
                upperCased(debtorIban),
                upperCased(creditorIban),
                upperCased(status),
                upperCased(currency),
                executionBound(executionFrom, START_OF_DAY),
                executionBound(executionTo, END_OF_DAY),
                SearchLimit.capped(requestedLimit));
    }

    private static LocalDateTime executionBound(String value, String timeOfDateOnlyBound) {
        if (isBlank(value)) {
            return null;
        }
        String text = value.trim().replace('T', ' ');
        if (text.length() == DATE_ONLY_LENGTH) {
            text = text + timeOfDateOnlyBound;
        }
        return LenientTimestamp.parse(text).orElse(null);
    }

    private static String upperCased(String value) {
        return isBlank(value) ? null : value.trim().toUpperCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
