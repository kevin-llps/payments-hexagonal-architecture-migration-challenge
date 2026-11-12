package fr.kevin.llps.payments.domain.sharedkernel;

/**
 * A date filter that does not follow the legacy date grammar. The message is {@code null} for a shape or range error
 * (rendered "Bad request" today) and the number-format message for a non-numeric field.
 */
public class MalformedDateException extends RuntimeException {

    public MalformedDateException(String message) {
        super(message);
    }
}
