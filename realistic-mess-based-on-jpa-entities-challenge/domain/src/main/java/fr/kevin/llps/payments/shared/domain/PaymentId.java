package fr.kevin.llps.payments.shared.domain;

import java.util.UUID;

/**
 * Identifier of a payment or recall, taken from the ingested payload.
 */
public record PaymentId(UUID value) {

    /**
     * Parses a raw identifier. The value is not trimmed: callers that accept padded input trim it themselves.
     */
    public static PaymentId parse(String raw) {
        try {
            return new PaymentId(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new MalformedValueException("Invalid payment id: " + raw, e);
        }
    }
}
