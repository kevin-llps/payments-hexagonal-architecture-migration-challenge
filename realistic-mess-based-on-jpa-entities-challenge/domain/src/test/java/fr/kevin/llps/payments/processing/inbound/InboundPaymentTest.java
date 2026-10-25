package fr.kevin.llps.payments.processing.inbound;

import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class InboundPaymentTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);

    @Test
    void age_in_days_should_count_the_days_from_the_value_date_to_today() {
        // GIVEN
        InboundPayment payment = payment(LocalDate.of(2026, 10, 22));

        // WHEN / THEN
        assertThat(payment.ageInDays(TODAY)).isEqualTo(3L);
    }

    @Test
    void age_in_days_should_be_null_without_throwing_when_there_is_no_value_date() {
        // GIVEN
        InboundPayment payment = payment(null);

        // WHEN / THEN
        assertThatCode(() -> assertThat(payment.ageInDays(TODAY)).isNull()).doesNotThrowAnyException();
    }

    // Regression check: negative ages come from ChronoUnit.DAYS.between.
    @Test
    void age_in_days_should_be_negative_for_a_future_value_date() {
        assertThat(payment(LocalDate.of(2026, 10, 30)).ageInDays(TODAY)).isEqualTo(-5L);
    }

    private static InboundPayment payment(LocalDate valueDate) {
        return new InboundPayment(PaymentId.parse("00000000-0000-0000-0000-000000000301"), "CTR-1",
                Money.of(new BigDecimal("10.00"), "EUR"), "DE89", "ACME", "BNPAFRPP", valueDate, null, "RECEIVED", null);
    }
}
