package fr.kevin.llps.payments.domain.recall.query;

import java.time.LocalTime;
import java.util.Optional;

import fr.kevin.llps.payments.domain.recall.RecallType;
import fr.kevin.llps.payments.domain.recall.UnknownRecallTypeException;
import fr.kevin.llps.payments.domain.sharedkernel.CodeFilter;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.sharedkernel.SearchLimit;
import fr.kevin.llps.payments.domain.sharedkernel.TimestampBound;

/**
 * Search criteria of recalls (BR-RCL-01..06).
 */
public final class RecallSearchCriteria {

    private static final LocalTime START_OF_DAY = LocalTime.MIDNIGHT;
    private static final String UNKNOWN_TYPE_MESSAGE = "Unknown recall type: ";

    private final CodeFilter recallType;
    private final PaymentId originalPaymentId;
    private final CodeFilter originalPaymentType;
    private final CodeFilter reasonCode;
    private final CodeFilter status;
    private final TimestampBound initiatedFrom;
    private final TimestampBound initiatedTo;
    private final SearchLimit limit;

    private RecallSearchCriteria(CodeFilter recallType, PaymentId originalPaymentId, CodeFilter originalPaymentType,
            CodeFilter reasonCode, CodeFilter status, TimestampBound initiatedFrom, TimestampBound initiatedTo,
            SearchLimit limit) {
        this.recallType = recallType;
        this.originalPaymentId = originalPaymentId;
        this.originalPaymentType = originalPaymentType;
        this.reasonCode = reasonCode;
        this.status = status;
        this.initiatedFrom = initiatedFrom;
        this.initiatedTo = initiatedTo;
        this.limit = limit;
    }

    /**
     * Validates in the legacy order: the recall type first, then the original payment id; the request limit is
     * ignored and always 50.
     */
    public static RecallSearchCriteria from(String recallType, String originalPaymentId, String originalPaymentType,
            String reasonCode, String status, String initiatedFrom, String initiatedTo) {
        CodeFilter typeFilter = CodeFilter.from(recallType).orElse(null);
        if (typeFilter != null && !new RecallType(typeFilter.value()).isSearchable()) {
            throw new UnknownRecallTypeException(UNKNOWN_TYPE_MESSAGE + recallType);
        }
        PaymentId originalPayment = originalPaymentId == null || originalPaymentId.isBlank()
                ? null : PaymentId.parse(originalPaymentId.trim());
        return new RecallSearchCriteria(typeFilter, originalPayment,
                CodeFilter.from(originalPaymentType).orElse(null),
                CodeFilter.from(reasonCode).orElse(null),
                CodeFilter.from(status).orElse(null),
                TimestampBound.lenient(initiatedFrom, START_OF_DAY),
                TimestampBound.lenient(initiatedTo, START_OF_DAY),
                SearchLimit.fixedDefault());
    }

    public Optional<CodeFilter> recallType() {
        return Optional.ofNullable(recallType);
    }

    public Optional<PaymentId> originalPaymentId() {
        return Optional.ofNullable(originalPaymentId);
    }

    public Optional<CodeFilter> originalPaymentType() {
        return Optional.ofNullable(originalPaymentType);
    }

    public Optional<CodeFilter> reasonCode() {
        return Optional.ofNullable(reasonCode);
    }

    public Optional<CodeFilter> status() {
        return Optional.ofNullable(status);
    }

    public TimestampBound initiatedFrom() {
        return initiatedFrom;
    }

    public TimestampBound initiatedTo() {
        return initiatedTo;
    }

    public SearchLimit limit() {
        return limit;
    }
}
