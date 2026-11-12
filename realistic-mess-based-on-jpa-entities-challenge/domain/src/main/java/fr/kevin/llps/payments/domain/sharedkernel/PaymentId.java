package fr.kevin.llps.payments.domain.sharedkernel;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifier of every payment record, supplied by the producer (BR-SHR-01).
 */
public record PaymentId(UUID value) {

    public PaymentId {
        Objects.requireNonNull(value, "value");
    }

    public static PaymentId of(UUID value) {
        return new PaymentId(value);
    }

    /**
     * Parses with the {@link UUID#fromString(String)} grammar, without trimming; the error message is the JDK one
     * (BR-API-05, BR-RCL-03).
     */
    public static PaymentId parse(String raw) {
        try {
            return new PaymentId(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new InvalidPaymentIdException(e.getMessage());
        }
    }
}
