package fr.kevin.llps.payments.domain.sharedkernel;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class TimestampBoundTest {

    private static final LocalTime startOfDay = LocalTime.of(0, 0, 0);
    private static final LocalTime endOfDay = LocalTime.of(23, 59, 59);

    @Test
    void should_have_no_value_and_be_satisfiable_when_absent() {
        // GIVEN
        Optional<LocalDateTime> expectedValue = Optional.empty();

        // WHEN
        TimestampBound actual = TimestampBound.absent();

        // THEN
        assertThat(actual)
                .returns(expectedValue, TimestampBound::value)
                .returns(false, TimestampBound::isUnsatisfiable);
    }

    @ParameterizedTest(name = "{0}: \"{1}\" (date-only time {2}) gives {3}")
    @MethodSource("lenientParsedCases")
    void should_parse_the_lenient_jdbc_timestamp_grammar(
            String testCase, String raw, LocalTime dateOnlyTime, LocalDateTime expected) {
        // GIVEN
        String input = raw;

        // WHEN
        TimestampBound actual = TimestampBound.lenient(input, dateOnlyTime);

        // THEN
        assertThat(actual)
                .returns(Optional.of(expected), TimestampBound::value)
                .returns(false, TimestampBound::isUnsatisfiable);
    }

    static Stream<Arguments> lenientParsedCases() {
        return Stream.of(
                Arguments.of("space separator", "2026-10-16 08:00:00", startOfDay, LocalDateTime.of(2026, 10, 16, 8, 0)),
                Arguments.of("T separator", "2026-10-16T08:00:00", startOfDay, LocalDateTime.of(2026, 10, 16, 8, 0)),
                Arguments.of("surrounding whitespace is trimmed", "  2026-10-16T08:00:00  ", startOfDay, LocalDateTime.of(2026, 10, 16, 8, 0)),
                Arguments.of("date-only lower bound", "2026-10-16", startOfDay, LocalDateTime.of(2026, 10, 16, 0, 0)),
                Arguments.of("date-only upper bound", "2026-10-16", endOfDay, LocalDateTime.of(2026, 10, 16, 23, 59, 59)),
                Arguments.of("date-only time ignored when time is present", "2026-10-16 08:00:00", endOfDay, LocalDateTime.of(2026, 10, 16, 8, 0)),
                Arguments.of("single-digit fields", "2026-1-5 7:3:2", startOfDay, LocalDateTime.of(2026, 1, 5, 7, 3, 2)),
                Arguments.of("February 30 rolls over", "2026-02-30 00:00:00", startOfDay, LocalDateTime.of(2026, 3, 2, 0, 0)),
                Arguments.of("date-only February 31 rolls over", "2026-02-31", endOfDay, LocalDateTime.of(2026, 3, 3, 23, 59, 59)),
                Arguments.of("hour 24 rolls over", "2026-10-16 24:00:00", startOfDay, LocalDateTime.of(2026, 10, 17, 0, 0)),
                Arguments.of("hour, minute and second roll over", "2026-10-16 25:61:61", startOfDay, LocalDateTime.of(2026, 10, 17, 2, 2, 1)),
                Arguments.of("explicit plus sign on the hour", "2026-10-16 +8:00:00", startOfDay, LocalDateTime.of(2026, 10, 16, 8, 0)),
                Arguments.of("negative hour rolls back", "2026-10-16 -1:00:00", startOfDay, LocalDateTime.of(2026, 10, 15, 23, 0)),
                Arguments.of("one fractional digit", "2026-10-16 08:00:00.5", startOfDay, LocalDateTime.of(2026, 10, 16, 8, 0, 0, 500_000_000)),
                Arguments.of("nine fractional digits", "2026-10-16 08:00:00.123456789", startOfDay, LocalDateTime.of(2026, 10, 16, 8, 0, 0, 123_456_789)),
                Arguments.of("DST gap wall time is kept (zone resolution belongs to the adapter)", "2026-03-29 02:30:00", startOfDay, LocalDateTime.of(2026, 3, 29, 2, 30)),
                Arguments.of("DST overlap wall time is kept", "2026-10-25 02:30:00", startOfDay, LocalDateTime.of(2026, 10, 25, 2, 30))
        );
    }

    @ParameterizedTest(name = "{0}: \"{1}\" gives no bound")
    @CsvSource(value = {
            "null input,                       ",
            "whitespace only,                  '   '",
            "single-digit date-only,           2026-1-5",
            "month 13,                         2026-13-01 00:00:00",
            "month 00,                         2026-00-10 00:00:00",
            "day 32,                           2026-10-32 00:00:00",
            "ten fractional digits,            2026-10-16 08:00:00.1234567891",
            "trailing dot,                     2026-10-16 08:00:00.",
            "trailing text,                    2026-10-16 08:00:00 extra",
            "trailing Z,                       2026-10-16T08:00:00Z",
            "date-only with Z,                 2026-10-16Z",
            "missing seconds,                  2026-10-16 08:00",
            "double space separator,           2026-10-16  08:00:00",
            "not a timestamp,                  abc"
    })
    void should_give_no_bound_when_the_lenient_grammar_does_not_match(String testCase, String raw) {
        // GIVEN
        String input = raw;

        // WHEN
        TimestampBound actual = TimestampBound.lenient(input, startOfDay);

        // THEN
        assertThat(actual)
                .returns(Optional.empty(), TimestampBound::value)
                .returns(false, TimestampBound::isUnsatisfiable);
    }

    @ParameterizedTest(name = "{0}: \"{1}\" gives {2}")
    @MethodSource("strictParsedCases")
    void should_parse_the_lenient_api_timestamp_format(String testCase, String raw, LocalDateTime expected) {
        // GIVEN
        String input = raw;

        // WHEN
        TimestampBound actual = TimestampBound.strictApiFormat(input);

        // THEN
        assertThat(actual)
                .returns(Optional.of(expected), TimestampBound::value)
                .returns(false, TimestampBound::isUnsatisfiable);
    }

    static Stream<Arguments> strictParsedCases() {
        return Stream.of(
                Arguments.of("well-formed", "2026-10-15T09:15:00", LocalDateTime.of(2026, 10, 15, 9, 15)),
                Arguments.of("surrounding whitespace is trimmed", "  2026-10-15T09:15:00  ", LocalDateTime.of(2026, 10, 15, 9, 15)),
                Arguments.of("single-digit fields", "2026-1-5T7:3:2", LocalDateTime.of(2026, 1, 5, 7, 3, 2)),
                Arguments.of("month 13 rolls over", "2026-13-01T00:00:00", LocalDateTime.of(2027, 1, 1, 0, 0)),
                Arguments.of("February 30 rolls over", "2026-02-30T00:00:00", LocalDateTime.of(2026, 3, 2, 0, 0)),
                Arguments.of("month and day 00 roll back", "2026-00-00T00:00:00", LocalDateTime.of(2025, 11, 30, 0, 0)),
                Arguments.of("hour 24 rolls over", "2026-10-16T24:00:00", LocalDateTime.of(2026, 10, 17, 0, 0)),
                Arguments.of("hour, minute and second roll over", "2026-10-16T25:61:61", LocalDateTime.of(2026, 10, 17, 2, 2, 1)),
                Arguments.of("negative month rolls back", "2026--1-16T08:00:00", LocalDateTime.of(2025, 11, 16, 8, 0)),
                Arguments.of("fractional seconds are ignored", "2026-10-16T08:00:00.999", LocalDateTime.of(2026, 10, 16, 8, 0)),
                Arguments.of("trailing Z is ignored", "2026-10-16T08:00:00Z", LocalDateTime.of(2026, 10, 16, 8, 0)),
                Arguments.of("trailing text is ignored", "2026-10-16T08:00:00 extra", LocalDateTime.of(2026, 10, 16, 8, 0)),
                Arguments.of("whitespace before a field is skipped", "2026- 10-16T08:00:00", LocalDateTime.of(2026, 10, 16, 8, 0)),
                Arguments.of("extra digits in the seconds", "2026-10-16T08:00:0012", LocalDateTime.of(2026, 10, 16, 8, 0, 12)),
                Arguments.of("five-digit year with a leading zero", "02026-10-16T08:00:00", LocalDateTime.of(2026, 10, 16, 8, 0)),
                Arguments.of("two-digit year is taken literally", "26-10-16T08:00:00", LocalDateTime.of(26, 10, 16, 8, 0)),
                Arguments.of("exponent in the seconds", "2026-10-16T08:00:01E2", LocalDateTime.of(2026, 10, 16, 8, 1, 40)),
                Arguments.of("exponent in the year", "2026E1-10-16T08:00:00", LocalDateTime.of(20260, 10, 16, 8, 0)),
                Arguments.of("DST gap wall time is kept (zone resolution belongs to the adapter)", "2026-03-29T02:30:00", LocalDateTime.of(2026, 3, 29, 2, 30)),
                Arguments.of("DST overlap wall time is kept", "2026-10-25T02:30:00", LocalDateTime.of(2026, 10, 25, 2, 30))
        );
    }

    @ParameterizedTest(name = "{0}: \"{1}\" gives an unsatisfiable bound")
    @CsvSource({
            "date-only,                2026-10-15",
            "space separator,          2026-10-16 08:00:00",
            "lower-case t separator,   2026-10-16t08:00:00",
            "missing seconds,          2026-10-16T08:00",
            "slash separators,         2026/10/16T08:00:00",
            "leading plus sign,        +2026-10-16T08:00:00",
            "not a date,               not-a-date"
    })
    void should_be_unsatisfiable_when_the_api_format_does_not_match(String testCase, String raw) {
        // GIVEN
        String input = raw;

        // WHEN
        TimestampBound actual = TimestampBound.strictApiFormat(input);

        // THEN
        assertThat(actual)
                .returns(Optional.empty(), TimestampBound::value)
                .returns(true, TimestampBound::isUnsatisfiable);
    }

    @ParameterizedTest(name = "{0}: \"{1}\" gives no bound")
    @CsvSource(value = {
            "null input,      ",
            "whitespace only, '   '"
    })
    void should_give_no_bound_when_the_api_timestamp_is_blank(String testCase, String raw) {
        // GIVEN
        String input = raw;

        // WHEN
        TimestampBound actual = TimestampBound.strictApiFormat(input);

        // THEN
        assertThat(actual)
                .returns(Optional.empty(), TimestampBound::value)
                .returns(false, TimestampBound::isUnsatisfiable);
    }
}
