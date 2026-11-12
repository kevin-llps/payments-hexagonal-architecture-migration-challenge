package fr.kevin.llps.payments.adapter.in.rest;

import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.kevin.llps.payments.domain.instant.InvalidIbanFilterException;
import fr.kevin.llps.payments.domain.recall.UnknownRecallTypeException;
import fr.kevin.llps.payments.domain.sharedkernel.InvalidPaymentIdException;
import fr.kevin.llps.payments.domain.sharedkernel.MalformedDateException;
import fr.kevin.llps.payments.domain.sharedkernel.NegativeSearchLimitException;

/**
 * Maps domain exceptions to the legacy 400 bodies, one explicit handler per exception. It runs before the legacy
 * catch-all advice (TL-R1).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestDomainExceptionHandler {

    private static final String ERROR_KEY = "error";
    private static final String DEFAULT_BAD_REQUEST_MESSAGE = "Bad request";

    @ExceptionHandler(InvalidPaymentIdException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPaymentId(InvalidPaymentIdException exception) {
        return badRequest(exception.getMessage());
    }

    @ExceptionHandler(InvalidIbanFilterException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidIbanFilter(InvalidIbanFilterException exception) {
        return badRequest(exception.getMessage());
    }

    @ExceptionHandler(UnknownRecallTypeException.class)
    public ResponseEntity<Map<String, Object>> handleUnknownRecallType(UnknownRecallTypeException exception) {
        return badRequest(exception.getMessage());
    }

    @ExceptionHandler(MalformedDateException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedDate(MalformedDateException exception) {
        // F-01: a non-numeric field keeps its number-format message, a shape or range error has none.
        String message = exception.getMessage();
        return badRequest(message == null ? DEFAULT_BAD_REQUEST_MESSAGE : message);
    }

    @ExceptionHandler(NegativeSearchLimitException.class)
    public ResponseEntity<Map<String, Object>> handleNegativeSearchLimit(NegativeSearchLimitException exception) {
        return badRequest(exception.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, message));
    }
}
