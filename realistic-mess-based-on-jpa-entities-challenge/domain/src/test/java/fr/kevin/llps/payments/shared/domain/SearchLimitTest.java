package fr.kevin.llps.payments.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SearchLimitTest {

    @Test
    void capped_should_default_to_50_when_no_limit_is_requested() {
        // GIVEN
        Integer requested = null;

        // WHEN
        SearchLimit limit = SearchLimit.capped(requested);

        // THEN
        assertThat(limit).isEqualTo(new SearchLimit(50));
    }

    @ParameterizedTest(name = "[{index}] requested={0}")
    @ValueSource(ints = {201, 1000})
    void capped_should_cap_at_200_when_requested_limit_is_above_200(int requested) {
        // GIVEN requested limit above the cap

        // WHEN
        SearchLimit limit = SearchLimit.capped(requested);

        // THEN
        assertThat(limit).isEqualTo(new SearchLimit(200));
    }

    @ParameterizedTest(name = "[{index}] requested={0}")
    @ValueSource(ints = {200, 1, 0, -1})
    void capped_should_keep_requested_limit_when_it_is_not_above_200(int requested) {
        // GIVEN requested limit within the cap (no lower bound, as in legacy)

        // WHEN
        SearchLimit limit = SearchLimit.capped(requested);

        // THEN
        assertThat(limit).isEqualTo(new SearchLimit(requested));
    }

    @Test
    void uncapped_should_default_to_50_when_no_limit_is_requested() {
        // GIVEN
        Integer requested = null;

        // WHEN
        SearchLimit limit = SearchLimit.uncapped(requested);

        // THEN
        assertThat(limit).isEqualTo(new SearchLimit(50));
    }

    @ParameterizedTest(name = "[{index}] requested={0}")
    @ValueSource(ints = {201, 1000, 1})
    void uncapped_should_keep_requested_limit_even_above_200(int requested) {
        // GIVEN any requested limit (inbound search has no cap)

        // WHEN
        SearchLimit limit = SearchLimit.uncapped(requested);

        // THEN
        assertThat(limit).isEqualTo(new SearchLimit(requested));
    }

    @Test
    void fixed_should_always_be_50() {
        // GIVEN no requested limit is taken into account (recall search)

        // WHEN
        SearchLimit limit = SearchLimit.fixed();

        // THEN
        assertThat(limit).isEqualTo(new SearchLimit(50));
    }
}
