package fr.kevin.llps.payments.processing.outbound;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ChargesTypeTest {

    @ParameterizedTest(name = "[{index}] code=\"{0}\" -> {1}")
    @CsvSource({"OUR, true", "our, false", "SHA, false", "'', false", ", false"})
    void is_our_should_be_true_only_for_the_exact_code_our(String code, boolean expected) {
        assertThat(new ChargesType(code).isOur()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] code=\"{0}\" -> {1}")
    @CsvSource({"SHA, true", "sha, false", "OUR, false", "'', false", ", false"})
    void is_sha_should_be_true_only_for_the_exact_code_sha(String code, boolean expected) {
        assertThat(new ChargesType(code).isSha()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] code=\"{0}\" -> {1}")
    @CsvSource({"BEN, true", "ben, false", "OUR, false", "'', false", ", false"})
    void is_ben_should_be_true_only_for_the_exact_code_ben(String code, boolean expected) {
        assertThat(new ChargesType(code).isBen()).isEqualTo(expected);
    }
}
