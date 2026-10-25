package fr.kevin.llps.payments.compliancehold;

import fr.kevin.llps.payments.shared.domain.SearchLimit;

import java.time.LocalDateTime;

/**
 * Normalized suspended payment search: every non-null value is an exact match, bounds are inclusive,
 * results are ordered by hold date, most recent first. An unsatisfiable search matches nothing.
 */
public record SuspendedPaymentSearchCriteria(String debtorIban,
                                             String holdReason,
                                             String reviewStatus,
                                             LocalDateTime heldFrom,
                                             LocalDateTime heldTo,
                                             boolean unsatisfiable,
                                             SearchLimit limit) {

    public static SuspendedPaymentSearchCriteria from(String debtorIban,
                                                      String holdReason,
                                                      String reviewStatus,
                                                      String heldFrom,
                                                      String heldTo,
                                                      Integer requestedLimit) {
        LocalDateTime from = heldBound(heldFrom);
        LocalDateTime to = heldBound(heldTo);
        return new SuspendedPaymentSearchCriteria(
                isBlank(debtorIban) ? null : debtorIban.trim(),
                normalizedCode(holdReason),
                normalizedCode(reviewStatus),
                from,
                to,
                (!isBlank(heldFrom) && from == null) || (!isBlank(heldTo) && to == null),
                SearchLimit.capped(requestedLimit));
    }

    private static LocalDateTime heldBound(String value) {
        return isBlank(value) ? null : LenientIsoTimestamp.parse(value.trim()).orElse(null);
    }

    private static String normalizedCode(String value) {
        return isBlank(value) ? null : value.trim().toUpperCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
