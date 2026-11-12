package fr.kevin.llps.payments.domain.sharedkernel;

public class InvalidPaymentIdException extends RuntimeException {

    public InvalidPaymentIdException(String message) {
        super(message);
    }
}
