package fr.kevin.llps.payments.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class IbanFilterTest {

    @ParameterizedTest(name = "[{index}] iban=\"{0}\"")
    @ValueSource(strings = {
            "NL91ABNA0417164",                     // 15 chars
            "MT84MALT011000012345MTLCAST001SABC",  // 34 chars
            "DE89 3704 0044 0532 0130 00",         // 22 chars once spaces are removed
            "de89370400440532013000"               // case is not checked
    })
    void is_plausible_should_return_true_when_length_without_spaces_is_between_15_and_34(String iban) {
        // GIVEN an IBAN filter of plausible length

        // WHEN
        boolean plausible = IbanFilter.isPlausible(iban);

        // THEN
        assertThat(plausible).isTrue();
    }

    @ParameterizedTest(name = "[{index}] iban=\"{0}\"")
    @ValueSource(strings = {
            "NL91ABNA041716",                       // 14 chars
            "MT84MALT011000012345MTLCAST001SABCD",  // 35 chars
            "NL91 ABNA 0417 16",                    // 14 chars once spaces are removed
            ""
    })
    void is_plausible_should_return_false_when_length_without_spaces_is_outside_15_to_34(String iban) {
        // GIVEN an IBAN filter that is too short or too long

        // WHEN
        boolean plausible = IbanFilter.isPlausible(iban);

        // THEN
        assertThat(plausible).isFalse();
    }

    @Test
    void is_plausible_should_return_false_without_throwing_when_iban_is_null() {
        // GIVEN
        String iban = null;

        // WHEN / THEN
        assertThatCode(() -> assertThat(IbanFilter.isPlausible(iban)).isFalse())
                .doesNotThrowAnyException();
    }
}
