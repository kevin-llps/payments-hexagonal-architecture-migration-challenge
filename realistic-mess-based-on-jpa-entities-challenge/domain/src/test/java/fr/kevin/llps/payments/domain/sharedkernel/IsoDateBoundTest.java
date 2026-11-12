package fr.kevin.llps.payments.domain.sharedkernel;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class IsoDateBoundTest {

    @ParameterizedTest(name = "{0}: \"{1}\" gives no bound")
    @CsvSource(value = {
            "null input,      ",
            "empty input,     ''",
            "whitespace only, '   '"
    })
    void should_give_no_bound_when_raw_is_blank(String testCase, String raw) {
        // GIVEN
        String input = raw;

        // WHEN
        Optional<IsoDateBound> actual = IsoDateBound.parse(input);

        // THEN
        assertThat(actual).isEmpty();
    }

    @ParameterizedTest(name = "{0}: \"{1}\" gives {2}")
    @CsvSource({
            "well-formed date,                     2026-10-16,       2026-10-16",
            "single-digit month and day,           2026-1-5,         2026-01-05",
            "surrounding whitespace is trimmed,    '  2026-01-05  ', 2026-01-05",
            "February 30 rolls over to March 2,    2026-02-30,       2026-03-02",
            "February 31 rolls over to March 3,    2026-02-31,       2026-03-03",
            "explicit plus sign on the month,      2026-+1-05,       2026-01-05"
    })
    void should_parse_the_legacy_iso_date_grammar(String testCase, String raw, LocalDate expected) {
        // GIVEN
        String input = raw;

        // WHEN
        Optional<IsoDateBound> actual = IsoDateBound.parse(input);

        // THEN
        assertThat(actual).contains(new IsoDateBound(expected));
    }

    @ParameterizedTest(name = "{0}: \"{1}\" is malformed without message")
    @CsvSource({
            "month 13,                     2026-13-01",
            "month 00,                     2026-00-10",
            "day 32,                       2026-10-32",
            "day 00,                       2026-10-00",
            "timestamp instead of a date,  2026-10-16T00:00:00",
            "missing day,                  2026-10",
            "two-digit year,               26-10-16",
            "negative month,               2026--1-05",
            "three-digit month,            2026-010-05",
            "not a date,                   abc",
            "slash separators,             2026/10/16"
    })
    void should_reject_a_malformed_date_without_message(String testCase, String raw) {
        // GIVEN
        String input = raw;

        // WHEN
        Throwable actual = catchThrowable(() -> IsoDateBound.parse(input));

        // THEN
        assertThat(actual).isInstanceOf(MalformedDateException.class).hasMessage(null);
    }

    @ParameterizedTest(name = "{0}: \"{1}\" is malformed with message {2}")
    @CsvSource(delimiter = '|', value = {
            "non-digit in the month | 2026-1x-05 | Error at index 1 in: \"1x\"",
            "non-digit in the day   | 2026-10-1x | Error at index 1 in: \"1x\"",
            "non-digit in the year  | 20x6-10-16 | Error at index 2 in: \"20x6\""
    })
    void should_reject_a_non_numeric_date_field_with_the_legacy_number_format_message(
            String testCase, String raw, String expectedMessage) {
        // GIVEN
        String input = raw;

        // WHEN
        Throwable actual = catchThrowable(() -> IsoDateBound.parse(input));

        // THEN
        assertThat(actual).isInstanceOf(MalformedDateException.class).hasMessage(expectedMessage);
    }
}
