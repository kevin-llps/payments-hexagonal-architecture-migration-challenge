package fr.kevin.llps.payments.domain.inbound;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * A credit transfer received from another bank.
 */
public final class InboundPayment {

    private final PaymentId id;
    private final String creditTransferRef;
    private final Money money;
    private final String creditorIban;
    private final String creditorName;
    private final String originBic;
    private final LocalDate valueDate;
    private final LocalDate settlementDate;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private InboundPayment(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.creditTransferRef = builder.creditTransferRef;
        this.money = builder.money == null ? Money.of(null, null) : builder.money;
        this.creditorIban = builder.creditorIban;
        this.creditorName = builder.creditorName;
        this.originBic = builder.originBic;
        this.valueDate = builder.valueDate;
        this.settlementDate = builder.settlementDate;
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

    public Optional<String> creditTransferRef() {
        return Optional.ofNullable(creditTransferRef);
    }

    public Money money() {
        return money;
    }

    public Optional<String> creditorIban() {
        return Optional.ofNullable(creditorIban);
    }

    public Optional<String> creditorName() {
        return Optional.ofNullable(creditorName);
    }

    public Optional<String> originBic() {
        return Optional.ofNullable(originBic);
    }

    public Optional<LocalDate> valueDate() {
        return Optional.ofNullable(valueDate);
    }

    public Optional<LocalDate> settlementDate() {
        return Optional.ofNullable(settlementDate);
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

    /**
     * Whole days between the value date and {@code today}: negative for a future value date, empty without a value
     * date (BR-INB-07). The single computation used by both search and details (BR-INB-08).
     */
    public Optional<Long> ageInDaysOn(LocalDate today) {
        Objects.requireNonNull(today, "today");
        return valueDate().map(date -> ChronoUnit.DAYS.between(date, today));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof InboundPayment payment &&
                id.equals(payment.id) &&
                Objects.equals(creditTransferRef, payment.creditTransferRef) &&
                Objects.equals(money, payment.money) &&
                Objects.equals(creditorIban, payment.creditorIban) &&
                Objects.equals(creditorName, payment.creditorName) &&
                Objects.equals(originBic, payment.originBic) &&
                Objects.equals(valueDate, payment.valueDate) &&
                Objects.equals(settlementDate, payment.settlementDate) &&
                Objects.equals(status, payment.status) &&
                Objects.equals(createdAt, payment.createdAt) &&
                Objects.equals(updatedAt, payment.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, creditTransferRef, money, creditorIban, creditorName, originBic, valueDate, settlementDate, status, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "InboundPayment[id=" + id.value() + ", creditTransferRef=" + creditTransferRef + "]";
    }

    /**
     * Builds an immutable {@link InboundPayment}; every attribute but the id is optional.
     */
    public static final class Builder {

        private final PaymentId id;
        private String creditTransferRef;
        private Money money;
        private String creditorIban;
        private String creditorName;
        private String originBic;
        private LocalDate valueDate;
        private LocalDate settlementDate;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder(PaymentId id) {
            this.id = id;
        }

        public Builder creditTransferRef(String value) {
            this.creditTransferRef = value;
            return this;
        }

        public Builder money(Money value) {
            this.money = value;
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

        public Builder originBic(String value) {
            this.originBic = value;
            return this;
        }

        public Builder valueDate(LocalDate value) {
            this.valueDate = value;
            return this;
        }

        public Builder settlementDate(LocalDate value) {
            this.settlementDate = value;
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

        public InboundPayment build() {
            return new InboundPayment(this);
        }
    }
}
