package fr.kevin.llps.payments.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LenientDateTest {

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {
            "", "16/10/2026", "2026-10-1x", "26-10-16", "2026-010-16", "2026-10-016", "2026-13-01", "2026-00-01",
            "2026-10-32", "2026-10-00", "2026-10--1", "2026-10-16T00:00:00", "2026-10", "2026-10-", "2026-10-16 "
    })
    void parse_should_return_empty_when_date_value_of_would_reject_the_text(String text) {
        // GIVEN text Date.valueOf rejects

        // WHEN / THEN
        assertThat(LenientDate.parse(text)).isEmpty();
    }

    @Test
    void parse_should_return_the_date_when_text_is_yyyy_mm_dd() {
        // GIVEN
        String text = "2026-10-15";

        // WHEN / THEN
        assertThat(LenientDate.parse(text)).contains(LocalDate.of(2026, 10, 15));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource({
            "2026-1-5, 2026-01-05",
            "2026-10-6, 2026-10-06",
            "2026-+1-05, 2026-01-05",
            "2026-10-+5, 2026-10-05"
    })
    void parse_should_accept_one_digit_and_signed_month_or_day(String text, LocalDate expected) {
        // GIVEN one-digit or plus-signed month / day fields

        // WHEN / THEN
        assertThat(LenientDate.parse(text)).contains(expected);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource({
            "2026-02-31, 2026-03-03",
            "2026-04-31, 2026-05-01",
            "2025-02-29, 2025-03-01"
    })
    void parse_should_roll_an_out_of_range_day_over_into_the_next_month(String text, LocalDate expected) {
        // GIVEN a day beyond the end of its month, as java.util.Date normalizes it

        // WHEN / THEN
        assertThat(LenientDate.parse(text)).contains(expected);
    }

    /**
     * Conformance check (not a TDD rule): same answer as the legacy parser for every input above and a few more.
     */
    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {
            "2026-10-15", "2026-1-5", "2026-10-6", "2026-+1-05", "2026-10-+5", "2026-02-31", "2026-04-31",
            "2025-02-29", "2024-02-29", "+202-10-15", "0001-01-01", "9999-12-31", "2026-1-31", "2026-12-1",
            "", "16/10/2026", "2026-10-1x", "26-10-16", "2026-010-16", "2026-10-016", "2026-13-01", "2026-00-01",
            "2026-10-32", "2026-10-00", "2026-10--1", "2026-10-16T00:00:00", "2026-10", "2026-10-", "2026-10-16 ",
            "-202-10-15", "2026--1-05", "2026- 1-05"
    })
    void parse_should_agree_with_date_value_of(String text) {
        // GIVEN the legacy result
        Optional<LocalDate> legacy;
        try {
            legacy = Optional.of(Date.valueOf(text).toLocalDate());
        } catch (IllegalArgumentException e) {
            legacy = Optional.empty();
        }

        // WHEN / THEN
        assertThat(LenientDate.parse(text)).isEqualTo(legacy);
    }
}
