package fr.kevin.llps.payments.domain.inbound;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.inbound.port.out.LoadInboundPayments;
import fr.kevin.llps.payments.domain.inbound.query.InboundPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.inbound.query.InboundPaymentSearchQuery;
import fr.kevin.llps.payments.domain.sharedkernel.MalformedDateException;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchInboundPaymentsServiceTest {

    private static final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Europe/Paris"));
    private static final LocalDate today = LocalDate.of(2026, 11, 12);
    private static final InboundPaymentSearchQuery emptyQuery =
            new InboundPaymentSearchQuery(null, null, null, null, null, null, null);

    @Mock
    private LoadInboundPayments loadInboundPayments;

    private SearchInboundPaymentsService searchInboundPaymentsService;

    @BeforeEach
    void createService() {
        searchInboundPaymentsService = new SearchInboundPaymentsService(loadInboundPayments, fixedClock);
    }

    @Test
    void should_return_one_view_per_loaded_payment() {
        // GIVEN (remapped from PaymentServiceUnitTest.inboundSearch_delegatesToRepository)
        when(loadInboundPayments.search(any())).thenReturn(List.of(payment(null)));

        // WHEN
        List<InboundPaymentView> actual = searchInboundPaymentsService.search(emptyQuery);

        // THEN
        assertThat(actual).hasSize(1);
        verify(loadInboundPayments).search(any());
        verifyNoMoreInteractions(loadInboundPayments);
    }

    @ParameterizedTest(name = "{0}: value date {1} gives ageInDays {2}")
    @MethodSource("searchAgeCases")
    void should_compute_the_age_in_days_of_every_search_row(String testCase, LocalDate valueDate,
            Optional<Long> expectedAge) {
        // GIVEN
        when(loadInboundPayments.search(any())).thenReturn(List.of(payment(valueDate)));

        // WHEN
        List<InboundPaymentView> actual = searchInboundPaymentsService.search(emptyQuery);

        // THEN
        assertThat(actual).singleElement().returns(expectedAge, InboundPaymentView::ageInDays);
    }

    static Stream<Arguments> searchAgeCases() {
        return Stream.of(
                Arguments.of("remapped PaymentControllerWebTest.inboundSearch_computesAgeInDays", today.minusDays(2), Optional.of(2L)),
                Arguments.of("remapped should_return_negative_age_in_days_in_search_when_value_date_is_in_the_future", today.plusDays(4), Optional.of(-4L)),
                Arguments.of("remapped should_return_null_age_in_days_in_search_when_value_date_is_null", null, Optional.empty()),
                Arguments.of("remapped should_not_compute_age_in_days_in_service_search (BR-INB-08, centralized computation)", today.minusDays(3), Optional.of(3L))
        );
    }

    @Test
    void should_load_with_the_criteria_built_from_the_raw_query() {
        // GIVEN
        InboundPaymentSearchQuery query = new InboundPaymentSearchQuery("DE89370400440532013000", "bnpa", "settled",
                "eur", "2026-10-01", "2026-10-31", 201);
        ArgumentCaptor<InboundPaymentSearchCriteria> criteria = ArgumentCaptor.forClass(InboundPaymentSearchCriteria.class);

        // WHEN
        searchInboundPaymentsService.search(query);

        // THEN
        verify(loadInboundPayments).search(criteria.capture());
        assertThat(criteria.getValue())
                .returns(Optional.of("BNPA"), InboundPaymentSearchCriteria::originBicFragment)
                .returns(201, c -> c.limit().value());
        verifyNoMoreInteractions(loadInboundPayments);
    }

    @Test
    void should_not_load_anything_when_a_date_bound_is_malformed() {
        // GIVEN
        InboundPaymentSearchQuery query = new InboundPaymentSearchQuery(null, null, null, null, "garbage", null, null);

        // WHEN
        Throwable actual = catchThrowable(() -> searchInboundPaymentsService.search(query));

        // THEN
        assertThat(actual).isInstanceOf(MalformedDateException.class);
        verifyNoInteractions(loadInboundPayments);
    }

    private static InboundPayment payment(LocalDate valueDate) {
        return InboundPayment.builder(PaymentId.parse("00000000-0000-0000-0000-000000000301")).valueDate(valueDate).build();
    }
}
