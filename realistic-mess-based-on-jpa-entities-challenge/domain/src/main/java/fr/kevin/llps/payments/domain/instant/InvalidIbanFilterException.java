package fr.kevin.llps.payments.domain.instant;

/**
 * An instant-payment IBAN search filter whose length is invalid (BR-INS-01, BR-INS-02).
 */
public class InvalidIbanFilterException extends RuntimeException {

    public InvalidIbanFilterException(String message) {
        super(message);
    }
}
