package fr.kevin.llps.payments.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LenientTimestampTest {

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {
            "", "last week", "16/10/2026", "2026-10-16", "2026-10-16T12:00:00", "2026-10-16 12:00",
            "26-10-16 12:00:00", "2026-13-01 12:00:00", "2026-00-01 12:00:00", "2026-10-32 12:00:00",
            "2026-10-16 12:00:00.", "2026-10-16 12:00:00.1234567890", "2026-10-16 12:00:00.x",
            "2026-10-16  12:00:00", "2026-010-16 12:00:00"
    })
    void parse_should_return_empty_when_timestamp_value_of_would_reject_the_text(String text) {
        // GIVEN text Timestamp.valueOf rejects

        // WHEN / THEN
        assertThat(LenientTimestamp.parse(text)).isEmpty();
    }

    @Test
    void parse_should_return_the_date_time_when_text_is_yyyy_mm_dd_hh_mm_ss() {
        // GIVEN
        String text = "2026-10-16 12:00:00";

        // WHEN / THEN
        assertThat(LenientTimestamp.parse(text)).contains(LocalDateTime.of(2026, 10, 16, 12, 0, 0));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource({
            "2026-1-5 9:7:3, 2026-01-05T09:07:03",
            "2026-10-6 12:00:00, 2026-10-06T12:00:00",
            "' 2026-10-16 12:00:00 ', 2026-10-16T12:00:00"
    })
    void parse_should_accept_one_digit_fields_and_surrounding_spaces(String text, LocalDateTime expected) {
        // GIVEN text with one-digit month / day / time fields, or padded

        // WHEN / THEN
        assertThat(LenientTimestamp.parse(text)).contains(expected);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource({
            "2026-10-16 12:00:00.5, 2026-10-16T12:00:00.5",
            "2026-10-16 12:00:00.000001, 2026-10-16T12:00:00.000001",
            "2026-10-16 12:00:00.123456789, 2026-10-16T12:00:00.123456789"
    })
    void parse_should_read_up_to_nine_fraction_digits_as_nanoseconds(String text, LocalDateTime expected) {
        // GIVEN text with fractional seconds

        // WHEN / THEN
        assertThat(LenientTimestamp.parse(text)).contains(expected);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource({
            "2026-02-31 00:00:00, 2026-03-03T00:00:00",
            "2026-04-31 08:00:00, 2026-05-01T08:00:00",
            "2026-10-16 25:00:00, 2026-10-17T01:00:00",
            "2026-10-16 12:61:00, 2026-10-16T13:01:00",
            "2026-10-16 12:00:60, 2026-10-16T12:01:00",
            "2026-10-16 -1:00:00, 2026-10-15T23:00:00"
    })
    void parse_should_roll_over_out_of_range_day_and_time_fields(String text, LocalDateTime expected) {
        // GIVEN text whose day or time fields overflow, as java.util.Date normalizes them

        // WHEN / THEN
        assertThat(LenientTimestamp.parse(text)).contains(expected);
    }

    /**
     * Conformance check (not a TDD rule): same answer as the legacy parser for every input above and a few more.
     * Local times inside a DST gap are excluded: java.util.Date shifts them, LocalDateTime does not; the persistence
     * adapter binds bounds through Timestamp.valueOf(LocalDateTime), which re-applies that shift.
     */
    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {
            "2026-10-16 12:00:00", "2026-1-5 9:7:3", " 2026-10-16 12:00:00 ", "2026-10-16 12:00:00.5",
            "2026-10-16 12:00:00.000001", "2026-10-16 12:00:00.123456789", "2026-02-31 00:00:00",
            "2026-04-31 08:00:00", "2026-10-16 25:00:00", "2026-10-16 12:61:00", "2026-10-16 12:00:60",
            "2026-10-16 -1:00:00", "2026-10-16 +5:00:00", "2024-02-29 00:00:00", "2025-02-29 00:00:00",
            "0001-01-01 00:00:00", "9999-12-31 23:59:59", "2026-10-16 12:00:00 extra", "2026-10-16 1:2:3.04",
            "", "last week", "16/10/2026", "2026-10-16", "2026-10-16T12:00:00", "2026-10-16 12:00",
            "26-10-16 12:00:00", "2026-13-01 12:00:00", "2026-00-01 12:00:00", "2026-10-32 12:00:00",
            "2026-10-16 12:00:00.", "2026-10-16 12:00:00.1234567890", "2026-10-16 12:00:00.x",
            "2026-10-16  12:00:00", "2026-010-16 12:00:00", "2026-10-16 12::00", "-2026-10-16 12:00:00"
    })
    void parse_should_agree_with_timestamp_value_of(String text) {
        // GIVEN the legacy result
        Optional<LocalDateTime> legacy;
        try {
            legacy = Optional.of(Timestamp.valueOf(text).toLocalDateTime());
        } catch (IllegalArgumentException e) {
            legacy = Optional.empty();
        }

        // WHEN / THEN
        assertThat(LenientTimestamp.parse(text)).isEqualTo(legacy);
    }
}
