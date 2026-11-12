package fr.kevin.llps.payments.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Shared REST error contract (BR-API-02, BR-API-03, BR-API-07; risk R-10).
 * Repository argument errors translated by Spring are mapped to 400 (user decision 2026-11-12).
 */
class RestGlobalExceptionHandlerTest {

    private final RestGlobalExceptionHandler handler = new RestGlobalExceptionHandler();

    @Test
    void should_map_illegal_argument_to_400_with_its_message_as_error() {
        // GIVEN
        IllegalArgumentException exception = new IllegalArgumentException("Invalid debtor IBAN");

        // WHEN
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(exception);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Invalid debtor IBAN"));
    }

    @Test
    void should_map_illegal_argument_without_message_to_400_with_default_message() {
        // GIVEN
        IllegalArgumentException exception = new IllegalArgumentException();

        // WHEN
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(exception);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Bad request"));
    }

    @Test
    void should_map_translated_illegal_argument_to_400_with_its_original_message() {
        // GIVEN
        IllegalArgumentException cause = new IllegalArgumentException("Invalid UUID string: xyz");
        InvalidDataAccessApiUsageException exception = new InvalidDataAccessApiUsageException(cause.getMessage(), cause);

        // WHEN
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidDataAccessApiUsage(exception);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Invalid UUID string: xyz"));
    }

    @Test
    void should_map_translated_illegal_argument_without_message_to_400_with_default_message() {
        // GIVEN
        IllegalArgumentException cause = new IllegalArgumentException();
        InvalidDataAccessApiUsageException exception = new InvalidDataAccessApiUsageException(null, cause);

        // WHEN
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidDataAccessApiUsage(exception);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Bad request"));
    }

    @Test
    void should_map_invalid_data_access_api_usage_not_caused_by_illegal_argument_to_500() {
        // GIVEN
        IllegalStateException cause = new IllegalStateException("transaction not active");
        InvalidDataAccessApiUsageException exception = new InvalidDataAccessApiUsageException(cause.getMessage(), cause);

        // WHEN
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidDataAccessApiUsage(exception);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .containsExactly(Map.entry("error", "Something went wrong, contact support with the trace id"));
    }

    @Test
    void should_map_any_other_exception_to_500_with_generic_message() {
        // GIVEN
        RuntimeException exception = new RuntimeException("db down");

        // WHEN
        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(exception);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .containsExactly(Map.entry("error", "Something went wrong, contact support with the trace id"));
    }
}
