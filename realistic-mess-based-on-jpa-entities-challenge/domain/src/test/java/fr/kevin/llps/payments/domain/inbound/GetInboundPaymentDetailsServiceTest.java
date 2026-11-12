package fr.kevin.llps.payments.domain.inbound;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.inbound.port.out.LoadInboundPayments;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetInboundPaymentDetailsServiceTest {

    private static final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Europe/Paris"));
    private static final LocalDate today = LocalDate.of(2026, 11, 12);
    private static final PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000301");

    @Mock
    private LoadInboundPayments loadInboundPayments;

    private GetInboundPaymentDetailsService getInboundPaymentDetailsService;

    @BeforeEach
    void createService() {
        getInboundPaymentDetailsService = new GetInboundPaymentDetailsService(loadInboundPayments, fixedClock);
    }

    @ParameterizedTest(name = "{0}: value date {1} gives ageInDays {2}")
    @MethodSource("detailsAgeCases")
    void should_return_the_payment_with_its_age_in_days(String testCase, LocalDate valueDate, Optional<Long> expectedAge) {
        // GIVEN
        InboundPayment payment = InboundPayment.builder(id).valueDate(valueDate).build();
        when(loadInboundPayments.findById(id)).thenReturn(Optional.of(payment));

        // WHEN
        Optional<InboundPaymentView> actual = getInboundPaymentDetailsService.getDetails(id);

        // THEN
        assertThat(actual).contains(new InboundPaymentView(payment, expectedAge.orElse(null)));
        verify(loadInboundPayments).findById(id);
        verifyNoMoreInteractions(loadInboundPayments);
    }

    static Stream<Arguments> detailsAgeCases() {
        return Stream.of(
                Arguments.of("remapped PaymentServiceUnitTest.inboundGetById_computesAgeInDays", today.minusDays(3), Optional.of(3L)),
                Arguments.of("remapped should_return_negative_age_in_days_when_value_date_is_in_the_future", today.plusDays(5), Optional.of(-5L)),
                Arguments.of("remapped should_leave_age_in_days_null_when_value_date_is_null", null, Optional.empty())
        );
    }

    @Test
    void should_return_empty_when_the_payment_does_not_exist() {
        // GIVEN (remapped from PaymentServiceUnitTest.inboundGetById_missing_returnsNull; unknown UUID)
        PaymentId unknownId = PaymentId.parse("11111111-1111-1111-1111-111111111111");
        when(loadInboundPayments.findById(unknownId)).thenReturn(Optional.empty());

        // WHEN
        Optional<InboundPaymentView> actual = getInboundPaymentDetailsService.getDetails(unknownId);

        // THEN
        assertThat(actual).isEmpty();
        verify(loadInboundPayments).findById(unknownId);
        verifyNoMoreInteractions(loadInboundPayments);
    }
}
