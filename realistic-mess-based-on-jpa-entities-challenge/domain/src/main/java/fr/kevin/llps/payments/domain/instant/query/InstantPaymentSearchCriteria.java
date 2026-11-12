package fr.kevin.llps.payments.domain.instant.query;

import java.time.LocalTime;
import java.util.Optional;

import fr.kevin.llps.payments.domain.instant.InvalidIbanFilterException;
import fr.kevin.llps.payments.domain.sharedkernel.CodeFilter;
import fr.kevin.llps.payments.domain.sharedkernel.Iban;
import fr.kevin.llps.payments.domain.sharedkernel.SearchLimit;
import fr.kevin.llps.payments.domain.sharedkernel.TimestampBound;

/**
 * Search criteria of instant payments (BR-INS-01..08). IBAN filters are contains-fragments (trimmed, upper-cased,
 * inner spaces kept); the persistence adapter turns them into {@code LIKE} patterns.
 */
public final class InstantPaymentSearchCriteria {

    private static final LocalTime START_OF_DAY = LocalTime.MIDNIGHT;
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);
    private static final String INVALID_DEBTOR_IBAN = "Invalid debtor IBAN";
    private static final String INVALID_CREDITOR_IBAN = "Invalid creditor IBAN";

    private final String endToEndId;
    private final String debtorIbanFragment;
    private final String creditorIbanFragment;
    private final CodeFilter status;
    private final CodeFilter currency;
    private final TimestampBound executionFrom;
    private final TimestampBound executionTo;
    private final SearchLimit limit;

    private InstantPaymentSearchCriteria(String endToEndId, String debtorIbanFragment, String creditorIbanFragment,
            CodeFilter status, CodeFilter currency, TimestampBound executionFrom, TimestampBound executionTo,
            SearchLimit limit) {
        this.endToEndId = endToEndId;
        this.debtorIbanFragment = debtorIbanFragment;
        this.creditorIbanFragment = creditorIbanFragment;
        this.status = status;
        this.currency = currency;
        this.executionFrom = executionFrom;
        this.executionTo = executionTo;
        this.limit = limit;
    }

    /**
     * Validates in the legacy order: debtor IBAN, creditor IBAN, then the limit last.
     */
    public static InstantPaymentSearchCriteria from(String endToEndId, String debtorIban, String creditorIban,
            String status, String currency, String executionFrom, String executionTo, Integer limit) {
        requireValidIban(debtorIban, INVALID_DEBTOR_IBAN);
        requireValidIban(creditorIban, INVALID_CREDITOR_IBAN);
        String exactEndToEndId = isBlank(endToEndId) ? null : endToEndId.trim();
        TimestampBound from = TimestampBound.lenient(executionFrom, START_OF_DAY);
        TimestampBound to = TimestampBound.lenient(executionTo, END_OF_DAY);
        return new InstantPaymentSearchCriteria(exactEndToEndId, fragmentOf(debtorIban), fragmentOf(creditorIban),
                CodeFilter.from(status).orElse(null), CodeFilter.from(currency).orElse(null), from, to,
                SearchLimit.capped(limit));
    }

    private static void requireValidIban(String raw, String message) {
        if (!isBlank(raw) && !Iban.of(raw).hasValidLength()) {
            throw new InvalidIbanFilterException(message);
        }
    }

    // Trimmed and upper-cased (default locale, as the legacy LIKE pattern); inner spaces are kept (N-03).
    private static String fragmentOf(String raw) {
        return isBlank(raw) ? null : raw.trim().toUpperCase();
    }

    private static boolean isBlank(String raw) {
        return raw == null || raw.isBlank();
    }

    public Optional<String> endToEndId() {
        return Optional.ofNullable(endToEndId);
    }

    public Optional<String> debtorIbanFragment() {
        return Optional.ofNullable(debtorIbanFragment);
    }

    public Optional<String> creditorIbanFragment() {
        return Optional.ofNullable(creditorIbanFragment);
    }

    public Optional<CodeFilter> status() {
        return Optional.ofNullable(status);
    }

    public Optional<CodeFilter> currency() {
        return Optional.ofNullable(currency);
    }

    public TimestampBound executionFrom() {
        return executionFrom;
    }

    public TimestampBound executionTo() {
        return executionTo;
    }

    public SearchLimit limit() {
        return limit;
    }
}
