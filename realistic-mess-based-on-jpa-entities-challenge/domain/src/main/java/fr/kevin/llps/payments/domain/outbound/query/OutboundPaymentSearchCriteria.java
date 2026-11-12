package fr.kevin.llps.payments.domain.outbound.query;

import java.util.Optional;

import fr.kevin.llps.payments.domain.sharedkernel.CodeFilter;
import fr.kevin.llps.payments.domain.sharedkernel.IsoDateBound;
import fr.kevin.llps.payments.domain.sharedkernel.SearchLimit;

/**
 * Search criteria of outbound payments (BR-OUT-01..05). The destination BIC is an upper-cased contains-fragment matched
 * against the column as stored (BR-OUT-02).
 */
public final class OutboundPaymentSearchCriteria {

    private final String debtorIban;
    private final String destinationBicFragment;
    private final CodeFilter chargesType;
    private final CodeFilter status;
    private final IsoDateBound executionFrom;
    private final IsoDateBound executionTo;
    private final SearchLimit limit;

    private OutboundPaymentSearchCriteria(String debtorIban, String destinationBicFragment, CodeFilter chargesType,
            CodeFilter status, IsoDateBound executionFrom, IsoDateBound executionTo, SearchLimit limit) {
        this.debtorIban = debtorIban;
        this.destinationBicFragment = destinationBicFragment;
        this.chargesType = chargesType;
        this.status = status;
        this.executionFrom = executionFrom;
        this.executionTo = executionTo;
        this.limit = limit;
    }

    /**
     * Validates in the legacy order: execution from, execution to, then the capped limit last.
     */
    public static OutboundPaymentSearchCriteria from(String debtorIban, String destinationBic, String chargesType,
            String status, String executionFrom, String executionTo, Integer limit) {
        IsoDateBound from = IsoDateBound.parse(executionFrom).orElse(null);
        IsoDateBound to = IsoDateBound.parse(executionTo).orElse(null);
        SearchLimit searchLimit = SearchLimit.capped(limit);
        return new OutboundPaymentSearchCriteria(trimmedOrNull(debtorIban), fragmentOf(destinationBic),
                CodeFilter.from(chargesType).orElse(null), CodeFilter.from(status).orElse(null), from, to, searchLimit);
    }

    private static String trimmedOrNull(String raw) {
        return raw == null || raw.isBlank() ? null : raw.trim();
    }

    // Trimmed and upper-cased (default locale, as the legacy LIKE pattern).
    private static String fragmentOf(String raw) {
        return raw == null || raw.isBlank() ? null : raw.trim().toUpperCase();
    }

    public Optional<String> debtorIban() {
        return Optional.ofNullable(debtorIban);
    }

    public Optional<String> destinationBicFragment() {
        return Optional.ofNullable(destinationBicFragment);
    }

    public Optional<CodeFilter> chargesType() {
        return Optional.ofNullable(chargesType);
    }

    public Optional<CodeFilter> status() {
        return Optional.ofNullable(status);
    }

    public Optional<IsoDateBound> executionFrom() {
        return Optional.ofNullable(executionFrom);
    }

    public Optional<IsoDateBound> executionTo() {
        return Optional.ofNullable(executionTo);
    }

    public SearchLimit limit() {
        return limit;
    }
}
