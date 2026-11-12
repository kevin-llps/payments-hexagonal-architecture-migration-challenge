package fr.kevin.llps.payments.domain.sharedkernel;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class IbanTest {

    @Test
    void should_keep_the_raw_value_unchanged_when_created() {
        // GIVEN
        String raw = " de89 3704 ";

        // WHEN
        Iban actual = Iban.of(raw);

        // THEN
        assertThat(actual).extracting(Iban::value).isEqualTo(" de89 3704 ");
    }

    @ParameterizedTest(name = "{0}: valid length is {2}")
    @MethodSource("lengthCases")
    void should_check_only_the_length_after_removing_spaces(String testCase, String raw, boolean expected) {
        // GIVEN
        Iban iban = new Iban(raw);

        // WHEN
        boolean actual = iban.hasValidLength();

        // THEN
        assertThat(actual).isEqualTo(expected);
    }

    static Stream<Arguments> lengthCases() {
        return Stream.of(
                Arguments.of("null iban", null, false),
                Arguments.of("too short DE12", "DE12", false),
                Arguments.of("14 characters", "X".repeat(14), false),
                Arguments.of("15 characters (minimum)", "X".repeat(15), true),
                Arguments.of("valid length DE89370400440532013000", "DE89370400440532013000", true),
                Arguments.of("spaces are removed before counting", "DE89 3704 0044 0532 0130 00", true),
                Arguments.of("34 characters (maximum)", "X".repeat(34), true),
                Arguments.of("35 characters", "X".repeat(35), false),
                Arguments.of("any characters, no checksum or country check", "!!!!!!!!!!!!!!!", true),
                Arguments.of("tabs are not removed (14 characters plus a tab)", "X".repeat(14) + "\t", true),
                Arguments.of("only spaces are removed (14 characters plus spaces)", "X".repeat(14) + "   ", false)
        );
    }
}
