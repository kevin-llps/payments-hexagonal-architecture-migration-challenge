package fr.kevin.llps.payments.domain.outbound;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * A credit transfer sent to another bank. The charges type is an open code (BR-SHR-10); fees are estimated by
 * {@link SearchFeePolicy} and {@link DetailsFeePolicy} (BR-OUT-06..08).
 */
public final class OutboundPayment {

    private final PaymentId id;
    private final String instructionId;
    private final Money money;
    private final String debtorIban;
    private final String debtorName;
    private final String destinationBic;
    private final LocalDate requestedExecutionDate;
    private final String chargesType;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private OutboundPayment(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.instructionId = builder.instructionId;
        this.money = builder.money == null ? Money.of(null, null) : builder.money;
        this.debtorIban = builder.debtorIban;
        this.debtorName = builder.debtorName;
        this.destinationBic = builder.destinationBic;
        this.requestedExecutionDate = builder.requestedExecutionDate;
        this.chargesType = builder.chargesType;
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

    public Optional<String> instructionId() {
        return Optional.ofNullable(instructionId);
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

    public Optional<String> destinationBic() {
        return Optional.ofNullable(destinationBic);
    }

    public Optional<LocalDate> requestedExecutionDate() {
        return Optional.ofNullable(requestedExecutionDate);
    }

    public Optional<String> chargesType() {
        return Optional.ofNullable(chargesType);
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
        return other instanceof OutboundPayment payment &&
                id.equals(payment.id) &&
                Objects.equals(instructionId, payment.instructionId) &&
                Objects.equals(money, payment.money) &&
                Objects.equals(debtorIban, payment.debtorIban) &&
                Objects.equals(debtorName, payment.debtorName) &&
                Objects.equals(destinationBic, payment.destinationBic) &&
                Objects.equals(requestedExecutionDate, payment.requestedExecutionDate) &&
                Objects.equals(chargesType, payment.chargesType) &&
                Objects.equals(status, payment.status) &&
                Objects.equals(createdAt, payment.createdAt) &&
                Objects.equals(updatedAt, payment.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, instructionId, money, debtorIban, debtorName, destinationBic, requestedExecutionDate, chargesType, status, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "OutboundPayment[id=" + id.value() + ", instructionId=" + instructionId + "]";
    }

    /**
     * Builds an immutable {@link OutboundPayment}; every attribute but the id is optional.
     */
    public static final class Builder {

        private final PaymentId id;
        private String instructionId;
        private Money money;
        private String debtorIban;
        private String debtorName;
        private String destinationBic;
        private LocalDate requestedExecutionDate;
        private String chargesType;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder(PaymentId id) {
            this.id = id;
        }

        public Builder instructionId(String value) {
            this.instructionId = value;
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

        public Builder destinationBic(String value) {
            this.destinationBic = value;
            return this;
        }

        public Builder requestedExecutionDate(LocalDate value) {
            this.requestedExecutionDate = value;
            return this;
        }

        public Builder chargesType(String value) {
            this.chargesType = value;
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

        public OutboundPayment build() {
            return new OutboundPayment(this);
        }
    }
}
