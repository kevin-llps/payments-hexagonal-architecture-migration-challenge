package fr.kevin.llps.payments.domain.sharedkernel;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PaymentIdTest {

    @Test
    void should_wrap_the_given_uuid_when_created_from_a_uuid() {
        // GIVEN
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000101");

        // WHEN
        PaymentId actual = PaymentId.of(uuid);

        // THEN
        assertThat(actual).extracting(PaymentId::value).isEqualTo(uuid);
    }

    @ParameterizedTest(name = "{0}: \"{1}\" parses to {2}")
    @CsvSource({
            "canonical form,                     00000000-0000-0000-0000-000000000101, 00000000-0000-0000-0000-000000000101",
            "lenient short groups of UUID grammar, 1-1-1-1-1,                          00000001-0001-0001-0001-000000000001"
    })
    void should_parse_the_uuid_grammar_of_the_legacy_api(String testCase, String raw, String expected) {
        // GIVEN
        UUID expectedUuid = UUID.fromString(expected);

        // WHEN
        PaymentId actual = PaymentId.parse(raw);

        // THEN
        assertThat(actual).extracting(PaymentId::value).isEqualTo(expectedUuid);
    }

    @ParameterizedTest(name = "{0}: \"{1}\" fails with \"{2}\"")
    @CsvSource(delimiter = '|', value = {
            "not a uuid                         | not-a-uuid                            | Invalid UUID string: not-a-uuid",
            "too many characters                | 00000000-0000-0000-0000-0000000001011 | UUID string too large",
            "leading space is not trimmed       | ' 00000000-0000-0000-0000-000000000101' | UUID string too large"
    })
    void should_throw_invalid_payment_id_with_the_legacy_message_when_raw_is_not_a_uuid(
            String testCase, String raw, String expectedMessage) {
        // GIVEN
        String input = raw;

        // WHEN
        Throwable actual = catchThrowable(() -> PaymentId.parse(input));

        // THEN
        assertThat(actual).isInstanceOf(InvalidPaymentIdException.class).hasMessage(expectedMessage);
    }
}
