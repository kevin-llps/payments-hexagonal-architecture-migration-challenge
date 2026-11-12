package fr.kevin.llps.payments.domain.suspended;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * A payment held for compliance review; {@code releasedAt} is empty while the hold is not released (BR-SUS-06).
 */
public final class SuspendedPayment {

    private final PaymentId id;
    private final Money money;
    private final String debtorIban;
    private final String debtorName;
    private final String creditorIban;
    private final String creditorName;
    private final String holdReason;
    private final String complianceCaseRef;
    private final String reviewStatus;
    private final LocalDateTime heldAt;
    private final LocalDateTime releasedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private SuspendedPayment(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.money = builder.money == null ? Money.of(null, null) : builder.money;
        this.debtorIban = builder.debtorIban;
        this.debtorName = builder.debtorName;
        this.creditorIban = builder.creditorIban;
        this.creditorName = builder.creditorName;
        this.holdReason = builder.holdReason;
        this.complianceCaseRef = builder.complianceCaseRef;
        this.reviewStatus = builder.reviewStatus;
        this.heldAt = builder.heldAt;
        this.releasedAt = builder.releasedAt;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public static Builder builder(PaymentId id) {
        return new Builder(id);
    }

    public PaymentId id() {
        return id;
    }

    public Money money() {
        return money;
    }

    public Optional<String> debtorIban() {
        return Optional.ofNullable(debtorIban);
    }

    public Optional<String> debtorName() {
        return Optional.ofNullable(debtorName);
    }

    public Optional<String> creditorIban() {
        return Optional.ofNullable(creditorIban);
    }

    public Optional<String> creditorName() {
        return Optional.ofNullable(creditorName);
    }

    public Optional<String> holdReason() {
        return Optional.ofNullable(holdReason);
    }

    public Optional<String> complianceCaseRef() {
        return Optional.ofNullable(complianceCaseRef);
    }

    public Optional<String> reviewStatus() {
        return Optional.ofNullable(reviewStatus);
    }

    public Optional<LocalDateTime> heldAt() {
        return Optional.ofNullable(heldAt);
    }

    public Optional<LocalDateTime> releasedAt() {
        return Optional.ofNullable(releasedAt);
    }

    public Optional<LocalDateTime> createdAt() {
        return Optional.ofNullable(createdAt);
    }

    public Optional<LocalDateTime> updatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SuspendedPayment payment
                && id.equals(payment.id)
                && money.equals(payment.money)
                && Objects.equals(debtorIban, payment.debtorIban)
                && Objects.equals(debtorName, payment.debtorName)
                && Objects.equals(creditorIban, payment.creditorIban)
                && Objects.equals(creditorName, payment.creditorName)
                && Objects.equals(holdReason, payment.holdReason)
                && Objects.equals(complianceCaseRef, payment.complianceCaseRef)
                && Objects.equals(reviewStatus, payment.reviewStatus)
                && Objects.equals(heldAt, payment.heldAt)
                && Objects.equals(releasedAt, payment.releasedAt)
                && Objects.equals(createdAt, payment.createdAt)
                && Objects.equals(updatedAt, payment.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, money, debtorIban, debtorName, creditorIban, creditorName, holdReason,
                complianceCaseRef, reviewStatus, heldAt, releasedAt, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "SuspendedPayment[id=" + id.value() + ", reviewStatus=" + reviewStatus + "]";
    }

    /**
     * Builds an immutable {@link SuspendedPayment}; every attribute but the id is optional.
     */
    public static final class Builder {

        private final PaymentId id;
        private Money money;
        private String debtorIban;
        private String debtorName;
        private String creditorIban;
        private String creditorName;
        private String holdReason;
        private String complianceCaseRef;
        private String reviewStatus;
        private LocalDateTime heldAt;
        private LocalDateTime releasedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder(PaymentId id) {
            this.id = id;
        }

        public Builder money(Money value) {
            this.money = value;
            return this;
        }

        public Builder debtorIban(String value) {
            this.debtorIban = value;
            return this;
        }

        public Builder debtorName(String value) {
            this.debtorName = value;
            return this;
        }

        public Builder creditorIban(String value) {
            this.creditorIban = value;
            return this;
        }

        public Builder creditorName(String value) {
            this.creditorName = value;
            return this;
        }

        public Builder holdReason(String value) {
            this.holdReason = value;
            return this;
        }

        public Builder complianceCaseRef(String value) {
            this.complianceCaseRef = value;
            return this;
        }

        public Builder reviewStatus(String value) {
            this.reviewStatus = value;
            return this;
        }

        public Builder heldAt(LocalDateTime value) {
            this.heldAt = value;
            return this;
        }

        public Builder releasedAt(LocalDateTime value) {
            this.releasedAt = value;
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

        public SuspendedPayment build() {
            return new SuspendedPayment(this);
        }
    }
}
