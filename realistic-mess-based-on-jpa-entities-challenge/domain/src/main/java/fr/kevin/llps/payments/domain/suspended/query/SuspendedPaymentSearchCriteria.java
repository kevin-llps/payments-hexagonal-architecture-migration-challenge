package fr.kevin.llps.payments.domain.suspended.query;

import java.util.Optional;

import fr.kevin.llps.payments.domain.sharedkernel.CodeFilter;
import fr.kevin.llps.payments.domain.sharedkernel.SearchLimit;
import fr.kevin.llps.payments.domain.sharedkernel.TimestampBound;

/**
 * Search criteria of suspended payments (BR-SUS-01..05).
 */
public final class SuspendedPaymentSearchCriteria {

    private final String debtorIban;
    private final CodeFilter holdReason;
    private final CodeFilter reviewStatus;
    private final TimestampBound heldFrom;
    private final TimestampBound heldTo;
    private final SearchLimit limit;

    private SuspendedPaymentSearchCriteria(String debtorIban, CodeFilter holdReason, CodeFilter reviewStatus,
            TimestampBound heldFrom, TimestampBound heldTo, SearchLimit limit) {
        this.debtorIban = debtorIban;
        this.holdReason = holdReason;
        this.reviewStatus = reviewStatus;
        this.heldFrom = heldFrom;
        this.heldTo = heldTo;
        this.limit = limit;
    }

    /**
     * Builds the criteria from raw request values: debtor IBAN trimmed and not validated, codes normalized, held
     * bounds in the API format, limit capped. The limit is validated even when the criteria match nothing.
     */
    public static SuspendedPaymentSearchCriteria from(String debtorIban, String holdReason, String reviewStatus,
            String heldFrom, String heldTo, Integer limit) {
        SearchLimit searchLimit = SearchLimit.capped(limit);
        String exactDebtorIban = debtorIban == null || debtorIban.isBlank() ? null : debtorIban.trim();
        return new SuspendedPaymentSearchCriteria(exactDebtorIban,
                CodeFilter.from(holdReason).orElse(null),
                CodeFilter.from(reviewStatus).orElse(null),
                TimestampBound.strictApiFormat(heldFrom),
                TimestampBound.strictApiFormat(heldTo),
                searchLimit);
    }

    public Optional<String> debtorIban() {
        return Optional.ofNullable(debtorIban);
    }

    public Optional<CodeFilter> holdReason() {
        return Optional.ofNullable(holdReason);
    }

    public Optional<CodeFilter> reviewStatus() {
        return Optional.ofNullable(reviewStatus);
    }

    public TimestampBound heldFrom() {
        return heldFrom;
    }

    public TimestampBound heldTo() {
        return heldTo;
    }

    public SearchLimit limit() {
        return limit;
    }

    /** True when a held bound is unparsable: such a search returns no payment (BR-SUS-04). */
    public boolean matchesNothing() {
        return heldFrom.isUnsatisfiable() || heldTo.isUnsatisfiable();
    }
}
