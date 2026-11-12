package fr.kevin.llps.payments.domain.inbound.query;

import java.util.Optional;

import fr.kevin.llps.payments.domain.sharedkernel.CodeFilter;
import fr.kevin.llps.payments.domain.sharedkernel.IsoDateBound;
import fr.kevin.llps.payments.domain.sharedkernel.SearchLimit;

/**
 * Search criteria of inbound payments (BR-INB-01..06). The origin BIC is a contains-fragment (trimmed, upper-cased);
 * the persistence adapter turns it into a {@code LIKE} pattern.
 */
public final class InboundPaymentSearchCriteria {

    private final String creditorIban;
    private final String originBicFragment;
    private final CodeFilter status;
    private final CodeFilter currency;
    private final IsoDateBound valueDateFrom;
    private final IsoDateBound valueDateTo;
    private final SearchLimit limit;

    private InboundPaymentSearchCriteria(String creditorIban, String originBicFragment, CodeFilter status,
            CodeFilter currency, IsoDateBound valueDateFrom, IsoDateBound valueDateTo, SearchLimit limit) {
        this.creditorIban = creditorIban;
        this.originBicFragment = originBicFragment;
        this.status = status;
        this.currency = currency;
        this.valueDateFrom = valueDateFrom;
        this.valueDateTo = valueDateTo;
        this.limit = limit;
    }

    /**
     * Validates in the legacy order: value date from, value date to, then the (uncapped) limit last.
     */
    public static InboundPaymentSearchCriteria from(String creditorIban, String originBic, String status,
            String currency, String valueDateFrom, String valueDateTo, Integer limit) {
        IsoDateBound from = IsoDateBound.parse(valueDateFrom).orElse(null);
        IsoDateBound to = IsoDateBound.parse(valueDateTo).orElse(null);
        SearchLimit searchLimit = SearchLimit.uncapped(limit);
        return new InboundPaymentSearchCriteria(trimmedOrNull(creditorIban), fragmentOf(originBic),
                CodeFilter.from(status).orElse(null), CodeFilter.from(currency).orElse(null), from, to, searchLimit);
    }

    private static String trimmedOrNull(String raw) {
        return raw == null || raw.isBlank() ? null : raw.trim();
    }

    // Trimmed and upper-cased (default locale, as the legacy LIKE pattern).
    private static String fragmentOf(String raw) {
        return raw == null || raw.isBlank() ? null : raw.trim().toUpperCase();
    }

    public Optional<String> creditorIban() {
        return Optional.ofNullable(creditorIban);
    }

    public Optional<String> originBicFragment() {
        return Optional.ofNullable(originBicFragment);
    }

    public Optional<CodeFilter> status() {
        return Optional.ofNullable(status);
    }

    public Optional<CodeFilter> currency() {
        return Optional.ofNullable(currency);
    }

    public Optional<IsoDateBound> valueDateFrom() {
        return Optional.ofNullable(valueDateFrom);
    }

    public Optional<IsoDateBound> valueDateTo() {
        return Optional.ofNullable(valueDateTo);
    }

    public SearchLimit limit() {
        return limit;
    }
}
