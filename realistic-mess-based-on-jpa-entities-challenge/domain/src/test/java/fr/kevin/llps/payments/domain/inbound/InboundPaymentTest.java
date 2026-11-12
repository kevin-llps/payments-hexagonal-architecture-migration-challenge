package fr.kevin.llps.payments.domain.inbound;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;

class InboundPaymentTest {

    private static final LocalDate today = LocalDate.of(2026, 11, 12);

    @ParameterizedTest(name = "{0}: value date {1} gives {2}")
    @MethodSource("ageCases")
    void should_compute_the_age_in_days_from_the_value_date_to_today(String testCase, LocalDate valueDate,
            Optional<Long> expected) {
        // GIVEN
        InboundPayment payment = InboundPayment.builder(PaymentId.parse("00000000-0000-0000-0000-000000000301"))
                .valueDate(valueDate)
                .build();

        // WHEN
        Optional<Long> actual = payment.ageInDaysOn(today);

        // THEN
        assertThat(actual).isEqualTo(expected);
    }

    static Stream<Arguments> ageCases() {
        return Stream.of(
                Arguments.of("value date three days ago", today.minusDays(3), Optional.of(3L)),
                Arguments.of("value date today", today, Optional.of(0L)),
                Arguments.of("future value date is negative", today.plusDays(5), Optional.of(-5L)),
                Arguments.of("no value date", null, Optional.empty())
        );
    }
}
