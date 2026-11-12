package fr.kevin.llps.payments.domain.sharedkernel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class SearchLimitTest {

    @ParameterizedTest(name = "{0}: requested {1} gives {2}")
    @CsvSource({
            "absent limit uses the default, ,     50",
            "zero is allowed,               0,    0",
            "value within bounds,           10,   10",
            "maximum is kept,               200,  200",
            "just above maximum is capped,  201,  200",
            "far above maximum is capped,   5000, 200"
    })
    void should_apply_the_capped_limit_policy(String testCase, Integer requested, int expected) {
        // GIVEN
        Integer input = requested;

        // WHEN
        SearchLimit actual = SearchLimit.capped(input);

        // THEN
        assertThat(actual).isEqualTo(new SearchLimit(expected));
    }

    @Test
    void should_reject_a_negative_capped_limit_with_the_legacy_message() {
        // GIVEN
        Integer negative = -1;

        // WHEN
        Throwable actual = catchThrowable(() -> SearchLimit.capped(negative));

        // THEN
        assertThat(actual).isInstanceOf(NegativeSearchLimitException.class)
                .hasMessage("max-results cannot be negative");
    }

    @ParameterizedTest(name = "{0}: requested {1} gives {2}")
    @CsvSource({
            "absent limit uses the default, ,    50",
            "zero is allowed,               0,   0",
            "above 200 is not capped,       500, 500"
    })
    void should_apply_the_uncapped_limit_policy(String testCase, Integer requested, int expected) {
        // GIVEN
        Integer input = requested;

        // WHEN
        SearchLimit actual = SearchLimit.uncapped(input);

        // THEN
        assertThat(actual).isEqualTo(new SearchLimit(expected));
    }

    @Test
    void should_reject_a_negative_uncapped_limit_with_the_legacy_message() {
        // GIVEN
        Integer negative = -5;

        // WHEN
        Throwable actual = catchThrowable(() -> SearchLimit.uncapped(negative));

        // THEN
        assertThat(actual).isInstanceOf(NegativeSearchLimitException.class)
                .hasMessage("max-results cannot be negative");
    }

    @Test
    void should_use_the_fixed_default_of_fifty() {
        // GIVEN
        int expected = 50;

        // WHEN
        SearchLimit actual = SearchLimit.fixedDefault();

        // THEN
        assertThat(actual).isEqualTo(new SearchLimit(expected));
    }
}
