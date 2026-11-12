package fr.kevin.llps.payments.domain.recall;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * A recall of an earlier payment; amount and currency are optional (BR-RCL-07/08, BR-SHR-09).
 */
public final class Recall {

    private final PaymentId id;
    private final String recallReference;
    private final String recallType;
    private final OriginalPaymentReference originalPayment;
    private final String reasonCode;
    private final Money money;
    private final LocalDateTime initiatedAt;
    private final LocalDate deadline;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Recall(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.recallReference = builder.recallReference;
        this.recallType = builder.recallType;
        this.originalPayment = builder.originalPayment == null
                ? OriginalPaymentReference.of(null, null) : builder.originalPayment;
        this.reasonCode = builder.reasonCode;
        this.money = builder.money == null ? Money.of(null, null) : builder.money;
        this.initiatedAt = builder.initiatedAt;
        this.deadline = builder.deadline;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public static Builder builder(PaymentId id) {
        return new Builder(id);
    }

    public PaymentId id() {
        return id;
    }

    public Optional<String> recallReference() {
        return Optional.ofNullable(recallReference);
    }

    public Optional<String> recallType() {
        return Optional.ofNullable(recallType);
    }

    public OriginalPaymentReference originalPayment() {
        return originalPayment;
    }

    public Optional<String> reasonCode() {
        return Optional.ofNullable(reasonCode);
    }

    public Money money() {
        return money;
    }

    public Optional<LocalDateTime> initiatedAt() {
        return Optional.ofNullable(initiatedAt);
    }

    public Optional<LocalDate> deadline() {
        return Optional.ofNullable(deadline);
    }

    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    public Optional<LocalDateTime> createdAt() {
        return Optional.ofNullable(createdAt);
    }

    public Optional<LocalDateTime> updatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Recall recall
                && id.equals(recall.id)
                && Objects.equals(recallReference, recall.recallReference)
                && Objects.equals(recallType, recall.recallType)
                && originalPayment.equals(recall.originalPayment)
                && Objects.equals(reasonCode, recall.reasonCode)
                && money.equals(recall.money)
                && Objects.equals(initiatedAt, recall.initiatedAt)
                && Objects.equals(deadline, recall.deadline)
                && Objects.equals(status, recall.status)
                && Objects.equals(createdAt, recall.createdAt)
                && Objects.equals(updatedAt, recall.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, recallReference, recallType, originalPayment, reasonCode, money, initiatedAt, deadline,
                status, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "Recall[id=" + id.value() + ", recallType=" + recallType + "]";
    }

    /**
     * Builds an immutable {@link Recall}; every attribute but the id is optional.
     */
    public static final class Builder {

        private final PaymentId id;
        private String recallReference;
        private String recallType;
        private OriginalPaymentReference originalPayment;
        private String reasonCode;
        private Money money;
        private LocalDateTime initiatedAt;
        private LocalDate deadline;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder(PaymentId id) {
            this.id = id;
        }

        public Builder recallReference(String value) {
            this.recallReference = value;
            return this;
        }

        public Builder recallType(String value) {
            this.recallType = value;
            return this;
        }

        public Builder originalPayment(OriginalPaymentReference value) {
            this.originalPayment = value;
            return this;
        }

        public Builder reasonCode(String value) {
            this.reasonCode = value;
            return this;
        }

        public Builder money(Money value) {
            this.money = value;
            return this;
        }

        public Builder initiatedAt(LocalDateTime value) {
            this.initiatedAt = value;
            return this;
        }

        public Builder deadline(LocalDate value) {
            this.deadline = value;
            return this;
        }

        public Builder status(String value) {
            this.status = value;
            return this;
        }

        public Builder createdAt(LocalDateTime value) {
            this.createdAt = value;
            return this;
        }

        public Builder updatedAt(LocalDateTime value) {
            this.updatedAt = value;
            return this;
        }

        public Recall build() {
            return new Recall(this);
        }
    }
}
