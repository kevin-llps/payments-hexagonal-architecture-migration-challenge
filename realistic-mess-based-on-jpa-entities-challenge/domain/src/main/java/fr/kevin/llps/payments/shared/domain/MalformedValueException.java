package fr.kevin.llps.payments.shared.domain;

/**
 * A raw value (identifier, date) cannot be parsed. Deliberately not an {@link IllegalArgumentException}.
 */
public class MalformedValueException extends RuntimeException {

    public MalformedValueException(String message, Throwable cause) {
        super(message, cause);
    }
}
