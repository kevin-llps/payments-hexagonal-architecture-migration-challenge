package fr.kevin.llps.payments.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentIdTest {

    private static final String RAW = "00000000-0000-0000-0000-000000000101";

    @Test
    void parse_should_return_payment_id_holding_the_uuid_when_raw_value_is_a_uuid() {
        // GIVEN
        String raw = RAW;

        // WHEN
        PaymentId paymentId = PaymentId.parse(raw);

        // THEN
        assertThat(paymentId).isEqualTo(new PaymentId(UUID.fromString(RAW)));
    }

    @ParameterizedTest(name = "[{index}] raw=\"{0}\"")
    @ValueSource(strings = {"not-a-uuid", "", " 00000000-0000-0000-0000-000000000101 "})
    void parse_should_throw_malformed_value_exception_when_raw_value_is_not_a_uuid(String raw) {
        // GIVEN raw value that UUID.fromString rejects (no trimming)

        // WHEN / THEN
        assertThatThrownBy(() -> PaymentId.parse(raw))
                .isInstanceOf(MalformedValueException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
