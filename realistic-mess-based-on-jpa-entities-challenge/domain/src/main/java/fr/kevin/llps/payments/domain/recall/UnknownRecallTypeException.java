package fr.kevin.llps.payments.domain.recall;

/**
 * A recall-type search filter outside {@link RecallType#SEARCHABLE_TYPES} (BR-RCL-01).
 */
public class UnknownRecallTypeException extends RuntimeException {

    public UnknownRecallTypeException(String message) {
        super(message);
    }
}
