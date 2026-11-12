package fr.kevin.llps.payments.adapter.in.rest;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestGlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RestGlobalExceptionHandler.class);

    private static final String DEFAULT_BAD_REQUEST_MESSAGE = "Bad request";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return badRequest(e.getMessage());
    }

    /**
     * Repositories are {@code @Repository} beans: Spring translates an {@link IllegalArgumentException} thrown there
     * (malformed UUID, malformed date, negative limit) into {@link InvalidDataAccessApiUsageException}. Such input
     * errors are mapped to 400 like any other {@link IllegalArgumentException}; any other cause stays a 500.
     */
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDataAccessApiUsage(InvalidDataAccessApiUsageException e) {
        if (e.getCause() instanceof IllegalArgumentException cause) {
            return badRequest(cause.getMessage());
        }
        return handleGeneric(e);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        String error = message == null ? DEFAULT_BAD_REQUEST_MESSAGE : message;
        return ResponseEntity.badRequest().body(Map.of("error", error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        LOG.error("unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Something went wrong, contact support with the trace id"));
    }
}
