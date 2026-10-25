package fr.kevin.llps.payments.shared.domain;

/**
 * A search filter value is rejected; the message is meant for the caller.
 */
public class InvalidSearchFilterException extends RuntimeException {

    public InvalidSearchFilterException(String message) {
        super(message);
    }
}
