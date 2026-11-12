package fr.kevin.llps.payments.domain.instant;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * An instant (SCT Inst) payment (BR-INS-09). The scheme defaults to {@link #DEFAULT_SCHEME} when an event does not
 * carry one (BR-ING-10, applied by the ingestion adapter).
 */
public final class InstantPayment {

    public static final String DEFAULT_SCHEME = "SCT INST";

    private final PaymentId id;
    private final String endToEndId;
    private final String uetr;
    private final Money money;
    private final String debtorIban;
    private final String debtorName;
    private final String creditorIban;
    private final String creditorName;
    private final LocalDateTime executionDatetime;
    private final String status;
    private final String scheme;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private InstantPayment(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.endToEndId = builder.endToEndId;
        this.uetr = builder.uetr;
        this.money = builder.money == null ? Money.of(null, null) : builder.money;
        this.debtorIban = builder.debtorIban;
        this.debtorName = builder.debtorName;
        this.creditorIban = builder.creditorIban;
        this.creditorName = builder.creditorName;
        this.executionDatetime = builder.executionDatetime;
        this.status = builder.status;
        this.scheme = builder.scheme;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public static Builder builder(PaymentId id) {
        return new Builder(id);
    }

    public PaymentId id() {
        return id;
    }

    public Optional<String> endToEndId() {
        return Optional.ofNullable(endToEndId);
    }

    public Optional<String> uetr() {
        return Optional.ofNullable(uetr);
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

    public Optional<LocalDateTime> executionDatetime() {
        return Optional.ofNullable(executionDatetime);
    }

    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    public Optional<String> scheme() {
        return Optional.ofNullable(scheme);
    }

    public Optional<LocalDateTime> createdAt() {
        return Optional.ofNullable(createdAt);
    }

    public Optional<LocalDateTime> updatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof InstantPayment payment &&
                id.equals(payment.id) &&
                Objects.equals(endToEndId, payment.endToEndId) &&
                Objects.equals(uetr, payment.uetr) &&
                Objects.equals(money, payment.money) &&
                Objects.equals(debtorIban, payment.debtorIban) &&
                Objects.equals(debtorName, payment.debtorName) &&
                Objects.equals(creditorIban, payment.creditorIban) &&
                Objects.equals(creditorName, payment.creditorName) &&
                Objects.equals(executionDatetime, payment.executionDatetime) &&
                Objects.equals(status, payment.status) &&
                Objects.equals(scheme, payment.scheme) &&
                Objects.equals(createdAt, payment.createdAt) &&
                Objects.equals(updatedAt, payment.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, endToEndId, uetr, money, debtorIban, debtorName, creditorIban, creditorName, executionDatetime, status, scheme, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "InstantPayment[id=" + id.value() + ", endToEndId=" + endToEndId + "]";
    }

    /**
     * Builds an immutable {@link InstantPayment}; every attribute but the id is optional.
     */
    public static final class Builder {

        private final PaymentId id;
        private String endToEndId;
        private String uetr;
        private Money money;
        private String debtorIban;
        private String debtorName;
        private String creditorIban;
        private String creditorName;
        private LocalDateTime executionDatetime;
        private String status;
        private String scheme;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder(PaymentId id) {
            this.id = id;
        }

        public Builder endToEndId(String value) {
            this.endToEndId = value;
            return this;
        }

        public Builder uetr(String value) {
            this.uetr = value;
            return this;
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

        public Builder executionDatetime(LocalDateTime value) {
            this.executionDatetime = value;
            return this;
        }

        public Builder status(String value) {
            this.status = value;
            return this;
        }

        public Builder scheme(String value) {
            this.scheme = value;
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

        public InstantPayment build() {
            return new InstantPayment(this);
        }
    }
}
