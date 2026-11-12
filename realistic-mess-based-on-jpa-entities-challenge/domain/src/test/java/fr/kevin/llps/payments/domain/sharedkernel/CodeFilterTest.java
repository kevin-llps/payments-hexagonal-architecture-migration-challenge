package fr.kevin.llps.payments.domain.sharedkernel;

import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CodeFilterTest {

    @ParameterizedTest(name = "{0}: \"{1}\" gives no filter")
    @CsvSource(value = {
            "null input,        ",
            "empty input,       ''",
            "whitespace only,   '   '"
    })
    void should_give_no_filter_when_raw_is_blank(String testCase, String raw) {
        // GIVEN
        String input = raw;

        // WHEN
        Optional<CodeFilter> actual = CodeFilter.from(input);

        // THEN
        assertThat(actual).isEmpty();
    }

    @ParameterizedTest(name = "{0}: \"{1}\" filters on {2}")
    @CsvSource({
            "surrounding spaces are trimmed and upper-cased, ' pending ', PENDING",
            "mixed case is upper-cased,                       Eur,         EUR",
            "already normalized code is kept,                 R-COR,       R-COR"
    })
    void should_trim_and_upper_case_the_raw_code(String testCase, String raw, String expected) {
        // GIVEN
        String input = raw;

        // WHEN
        Optional<CodeFilter> actual = CodeFilter.from(input);

        // THEN
        assertThat(actual).contains(new CodeFilter(expected));
    }
}
