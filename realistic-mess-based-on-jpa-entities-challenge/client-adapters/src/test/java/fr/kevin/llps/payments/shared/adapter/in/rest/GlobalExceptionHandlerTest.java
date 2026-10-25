package fr.kevin.llps.payments.shared.adapter.in.rest;

import fr.kevin.llps.payments.shared.domain.InvalidSearchFilterException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handle_invalid_search_filter_should_answer_400() {
        // GIVEN
        InvalidSearchFilterException exception = new InvalidSearchFilterException("Invalid debtor IBAN");

        // WHEN
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidSearchFilter(exception);

        // THEN
        assertThat(response).extracting(ResponseEntity::getStatusCode).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handle_invalid_search_filter_should_return_the_exception_message_as_error() {
        // GIVEN
        InvalidSearchFilterException exception = new InvalidSearchFilterException("Unknown recall type: R-XXX");

        // WHEN
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidSearchFilter(exception);

        // THEN
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Unknown recall type: R-XXX"));
    }

    @Test
    void handle_invalid_search_filter_should_be_selected_by_spring_mvc_instead_of_the_generic_handler() throws Exception {
        // GIVEN a controller rejecting a search filter
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new RejectingController())
                .setControllerAdvice(handler)
                .build();

        // WHEN / THEN
        mvc.perform(get("/rejecting"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid creditor IBAN"));
    }

    @RestController
    static class RejectingController {

        @GetMapping("/rejecting")
        String reject() {
            throw new InvalidSearchFilterException("Invalid creditor IBAN");
        }
    }
}
