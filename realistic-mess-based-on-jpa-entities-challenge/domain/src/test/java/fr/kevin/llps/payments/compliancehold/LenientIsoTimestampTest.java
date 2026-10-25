package fr.kevin.llps.payments.compliancehold;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LenientIsoTimestampTest {

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {
            "", "last week", "2026-10-16", "2026-10-16 12:00:00", "2026-10-16t12:00:00", "2026-10-16T12:00",
            "2026-10-16T12:00:", "T12:00:00", "2026-10-+1T00:00:00", "2026 -10-16T00:00:00"
    })
    void parse_should_return_empty_when_the_lenient_simple_date_format_would_reject_the_text(String text) {
        // GIVEN text the legacy format rejects

        // WHEN / THEN
        assertThat(LenientIsoTimestamp.parse(text)).isEmpty();
    }

    @Test
    void parse_should_return_the_date_time_when_text_is_yyyy_mm_dd_t_hh_mm_ss() {
        // GIVEN
        String text = "2026-10-16T12:00:00";

        // WHEN / THEN
        assertThat(LenientIsoTimestamp.parse(text)).contains(LocalDateTime.of(2026, 10, 16, 12, 0, 0));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource({
            "2026-1-5T9:7:3, 2026-01-05T09:07:03",
            "26-10-16T00:00:00, 0026-10-16T00:00:00",
            "20261-10-16T00:00:00, +20261-10-16T00:00:00"
    })
    void parse_should_accept_fields_of_any_length(String text, LocalDateTime expected) {
        // GIVEN one-digit fields or a year that is not four digits long

        // WHEN / THEN
        assertThat(LenientIsoTimestamp.parse(text)).contains(expected);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource({
            "2026-10-16T12:00:00Z, 2026-10-16T12:00:00",
            "2026-10-16T12:00:00.123, 2026-10-16T12:00:00",
            "2026-10-16T12:00:00 x, 2026-10-16T12:00:00",
            "2026-10-16T12:00:00:00, 2026-10-16T12:00:00",
            "2026-10-16T12:00:5x, 2026-10-16T12:00:05"
    })
    void parse_should_ignore_text_after_the_seconds(String text, LocalDateTime expected) {
        // GIVEN text continuing after the seconds field

        // WHEN / THEN
        assertThat(LenientIsoTimestamp.parse(text)).contains(expected);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource({
            "2026-13-01T00:00:00, 2027-01-01T00:00:00",
            "2026-00-10T00:00:00, 2025-12-10T00:00:00",
            "2026-02-31T00:00:00, 2026-03-03T00:00:00",
            "2026-10-16T25:00:00, 2026-10-17T01:00:00",
            "2026-10-16T12:00:60, 2026-10-16T12:01:00",
            "2026-10--1T00:00:00, 2026-09-29T00:00:00"
    })
    void parse_should_roll_over_out_of_range_and_negative_fields(String text, LocalDateTime expected) {
        // GIVEN fields outside their range, as a lenient calendar normalizes them

        // WHEN / THEN
        assertThat(LenientIsoTimestamp.parse(text)).contains(expected);
    }

    /**
     * Conformance check (not a TDD rule): same answer as the legacy lenient SimpleDateFormat for every input above and
     * a few more. Excluded: negative years and local times inside a DST gap (the persistence adapter binds bounds
     * through Timestamp.valueOf(LocalDateTime), which re-applies the DST shift).
     */
    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {
            "2026-10-16T12:00:00", "2026-1-5T9:7:3", "26-10-16T00:00:00", "20261-10-16T00:00:00",
            "2026-10-16T12:00:00Z", "2026-10-16T12:00:00.123", "2026-10-16T12:00:00 x", "2026-10-16T12:00:00:00",
            "2026-10-16T12:00:5x", "2026-13-01T00:00:00", "2026-00-10T00:00:00", "2026-02-31T00:00:00",
            "2026-10-16T25:00:00", "2026-10-16T12:00:60", "2026-10--1T00:00:00", "2026-10-16T-1:-1:-1",
            "2024-02-29T00:00:00", "2025-02-29T00:00:00", "2026-10-16T00:00:00-01:00", "2026-10-16T12:00:00+02:00",
            "", "last week", "2026-10-16", "2026-10-16 12:00:00", "2026-10-16t12:00:00", "2026-10-16T12:00",
            "2026-10-16T12:00:", "T12:00:00", "2026-10-+1T00:00:00", "2026 -10-16T00:00:00", "2026-10-16T--1:00:00",
            "16/10/2026"
    })
    void parse_should_agree_with_lenient_simple_date_format(String text) {
        // GIVEN the legacy result (PaymentUtils.parseTimestamp)
        Optional<LocalDateTime> legacy;
        try {
            legacy = Optional.of(new Timestamp(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(text).getTime())
                    .toLocalDateTime());
        } catch (ParseException e) {
            legacy = Optional.empty();
        }

        // WHEN / THEN
        assertThat(LenientIsoTimestamp.parse(text)).isEqualTo(legacy);
    }
}
