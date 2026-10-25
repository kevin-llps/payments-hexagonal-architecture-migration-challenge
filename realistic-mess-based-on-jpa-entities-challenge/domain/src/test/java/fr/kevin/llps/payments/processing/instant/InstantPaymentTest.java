package fr.kevin.llps.payments.processing.instant;

import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InstantPaymentTest {

    private static final PaymentId ID = PaymentId.parse("00000000-0000-0000-0000-000000000901");
    private static final Money AMOUNT = Money.of(new BigDecimal("42.00"), "EUR");

    @Test
    void record_should_default_a_missing_scheme_to_sct_inst() {
        // GIVEN / WHEN
        InstantPayment payment = InstantPayment.record(ID, "E2E-1", "uetr-1", AMOUNT, "DE89", "ACME", "FR76",
                "Dupont", null, "COMPLETED", null, null);

        // THEN
        assertThat(payment).isEqualTo(new InstantPayment(ID, "E2E-1", "uetr-1", AMOUNT, "DE89", "ACME", "FR76",
                "Dupont", null, "COMPLETED", "SCT INST", null));
    }

    @ParameterizedTest(name = "[{index}] scheme=\"{0}\"")
    @ValueSource(strings = {"SCT INST", "OTHER", ""})
    void record_should_keep_a_given_scheme_even_when_empty(String scheme) {
        // GIVEN / WHEN
        InstantPayment payment = InstantPayment.record(ID, "E2E-1", "uetr-1", AMOUNT, "DE89", "ACME", "FR76",
                "Dupont", null, "COMPLETED", scheme, null);

        // THEN
        assertThat(payment.scheme()).isEqualTo(scheme);
    }
}
