package fr.kevin.llps.payments.adapter.in.rest.outbound;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the outbound details date rendering byte for byte (TLD §8.3 as amended at the IT-06 gate: fixed expected strings
 * observed beforehand from the legacy Jackson rendering of {@code java.sql.Timestamp} / {@code java.sql.Date}, no
 * Jackson comparison in this test). The formatter receives its zone explicitly, so the JVM default zone is not changed.
 */
class RestOutboundPaymentDetailsFormatterTest {

    @ParameterizedTest(name = "{0} {1}: {2} -> {3}")
    @CsvSource({
            "UTC, winter, 2026-01-15T10:30, 2026-01-15T10:30:00.000+00:00",
            "UTC, summer, 2026-07-15T10:30, 2026-07-15T10:30:00.000+00:00",
            "UTC, zero millis (N-02 seed time), 2026-10-16T08:00, 2026-10-16T08:00:00.000+00:00",
            "UTC, non-zero millis, 2026-10-16T08:00:00.123, 2026-10-16T08:00:00.123+00:00",
            "UTC, sub-millisecond truncated, 2026-10-16T08:00:00.123456789, 2026-10-16T08:00:00.123+00:00",
            "UTC, Paris DST gap, 2026-03-29T02:30, 2026-03-29T02:30:00.000+00:00",
            "UTC, Paris DST overlap, 2026-10-25T02:30, 2026-10-25T02:30:00.000+00:00",
            "UTC, New York DST gap, 2026-03-08T02:30, 2026-03-08T02:30:00.000+00:00",
            "UTC, New York DST overlap, 2026-11-01T01:30, 2026-11-01T01:30:00.000+00:00",
            "Europe/Paris, winter, 2026-01-15T10:30, 2026-01-15T09:30:00.000+00:00",
            "Europe/Paris, summer, 2026-07-15T10:30, 2026-07-15T08:30:00.000+00:00",
            "Europe/Paris, zero millis (N-02 seed time), 2026-10-16T08:00, 2026-10-16T06:00:00.000+00:00",
            "Europe/Paris, non-zero millis, 2026-10-16T08:00:00.123, 2026-10-16T06:00:00.123+00:00",
            "Europe/Paris, sub-millisecond truncated, 2026-10-16T08:00:00.123456789, 2026-10-16T06:00:00.123+00:00",
            "Europe/Paris, Paris DST gap, 2026-03-29T02:30, 2026-03-29T01:30:00.000+00:00",
            "Europe/Paris, Paris DST overlap, 2026-10-25T02:30, 2026-10-25T01:30:00.000+00:00",
            "Europe/Paris, New York DST gap, 2026-03-08T02:30, 2026-03-08T01:30:00.000+00:00",
            "Europe/Paris, New York DST overlap, 2026-11-01T01:30, 2026-11-01T00:30:00.000+00:00",
            "America/New_York, winter, 2026-01-15T10:30, 2026-01-15T15:30:00.000+00:00",
            "America/New_York, summer, 2026-07-15T10:30, 2026-07-15T14:30:00.000+00:00",
            "America/New_York, zero millis (N-02 seed time), 2026-10-16T08:00, 2026-10-16T12:00:00.000+00:00",
            "America/New_York, non-zero millis, 2026-10-16T08:00:00.123, 2026-10-16T12:00:00.123+00:00",
            "America/New_York, sub-millisecond truncated, 2026-10-16T08:00:00.123456789, 2026-10-16T12:00:00.123+00:00",
            "America/New_York, Paris DST gap, 2026-03-29T02:30, 2026-03-29T06:30:00.000+00:00",
            "America/New_York, Paris DST overlap, 2026-10-25T02:30, 2026-10-25T06:30:00.000+00:00",
            "America/New_York, New York DST gap, 2026-03-08T02:30, 2026-03-08T07:30:00.000+00:00",
            "America/New_York, New York DST overlap, 2026-11-01T01:30, 2026-11-01T06:30:00.000+00:00",
            "Asia/Kolkata, winter, 2026-01-15T10:30, 2026-01-15T05:00:00.000+00:00",
            "Asia/Kolkata, summer, 2026-07-15T10:30, 2026-07-15T05:00:00.000+00:00",
            "Asia/Kolkata, zero millis (N-02 seed time), 2026-10-16T08:00, 2026-10-16T02:30:00.000+00:00",
            "Asia/Kolkata, non-zero millis, 2026-10-16T08:00:00.123, 2026-10-16T02:30:00.123+00:00",
            "Asia/Kolkata, sub-millisecond truncated, 2026-10-16T08:00:00.123456789, 2026-10-16T02:30:00.123+00:00",
            "Asia/Kolkata, Paris DST gap, 2026-03-29T02:30, 2026-03-28T21:00:00.000+00:00",
            "Asia/Kolkata, Paris DST overlap, 2026-10-25T02:30, 2026-10-24T21:00:00.000+00:00",
            "Asia/Kolkata, New York DST gap, 2026-03-08T02:30, 2026-03-07T21:00:00.000+00:00",
            "Asia/Kolkata, New York DST overlap, 2026-11-01T01:30, 2026-10-31T20:00:00.000+00:00"
    })
    void should_render_timestamps_like_the_legacy_jackson_output(String zone, String testCase, LocalDateTime value,
            String expected) {
        // GIVEN
        RestOutboundPaymentDetailsFormatter formatter = new RestOutboundPaymentDetailsFormatter(ZoneId.of(zone));

        // WHEN
        String actual = formatter.formatTimestamp(value);

        // THEN
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}: 2026-10-16 -> 2026-10-16")
    @CsvSource({"UTC", "Europe/Paris", "America/New_York", "Asia/Kolkata"})
    void should_render_dates_without_zone_conversion(String zone) {
        // GIVEN
        RestOutboundPaymentDetailsFormatter formatter = new RestOutboundPaymentDetailsFormatter(ZoneId.of(zone));

        // WHEN
        String actual = formatter.formatDate(LocalDate.of(2026, 10, 16));

        // THEN
        assertThat(actual).isEqualTo("2026-10-16");
    }

    @Test
    void should_keep_absent_values_null() {
        // GIVEN
        RestOutboundPaymentDetailsFormatter formatter = new RestOutboundPaymentDetailsFormatter(ZoneId.of("Europe/Paris"));

        // WHEN
        String timestamp = formatter.formatTimestamp(null);

        // THEN
        assertThat(timestamp).isNull();
        assertThat(formatter.formatDate(null)).isNull();
    }
}
