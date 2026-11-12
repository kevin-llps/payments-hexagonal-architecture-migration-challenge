package fr.kevin.llps.payments.domain.sharedkernel;

public class NegativeSearchLimitException extends RuntimeException {

    private static final String MESSAGE = "max-results cannot be negative";

    public NegativeSearchLimitException() {
        super(MESSAGE);
    }
}
