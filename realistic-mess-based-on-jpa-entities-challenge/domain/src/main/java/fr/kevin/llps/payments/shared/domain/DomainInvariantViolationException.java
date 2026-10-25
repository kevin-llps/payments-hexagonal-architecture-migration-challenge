package fr.kevin.llps.payments.shared.domain;

/**
 * A domain object cannot be built because one of its invariants does not hold.
 */
public class DomainInvariantViolationException extends RuntimeException {

    public DomainInvariantViolationException(String message) {
        super(message);
    }
}
