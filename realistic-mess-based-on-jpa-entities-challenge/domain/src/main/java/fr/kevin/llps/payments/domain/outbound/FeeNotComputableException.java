package fr.kevin.llps.payments.domain.outbound;

/**
 * The details fee of an {@code OUR} payment cannot be computed without an amount (BR-OUT-07). No dedicated REST
 * handler: it surfaces as the generic 500 like the legacy failure.
 */
public class FeeNotComputableException extends RuntimeException {

    public FeeNotComputableException(String message) {
        super(message);
    }
}
