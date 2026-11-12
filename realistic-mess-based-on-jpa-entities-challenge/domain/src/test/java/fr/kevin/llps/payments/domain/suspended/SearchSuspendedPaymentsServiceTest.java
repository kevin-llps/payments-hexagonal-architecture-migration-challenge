package fr.kevin.llps.payments.domain.suspended;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.sharedkernel.CodeFilter;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.sharedkernel.SearchLimit;
import fr.kevin.llps.payments.domain.suspended.port.out.LoadSuspendedPayments;
import fr.kevin.llps.payments.domain.suspended.query.SuspendedPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.suspended.query.SuspendedPaymentSearchQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchSuspendedPaymentsServiceTest {

    @Mock
    private LoadSuspendedPayments loadSuspendedPayments;

    @InjectMocks
    private SearchSuspendedPaymentsService searchSuspendedPaymentsService;

    @Test
    void should_return_the_loaded_payments_unchanged() {
        // GIVEN (remapped from PaymentServiceUnitTest.suspendedSearch_mapsAllFields)
        SuspendedPayment payment = SuspendedPayment.builder(PaymentId.of(UUID.fromString("00000000-0000-0000-0000-000000000201")))
                .money(Money.of(new BigDecimal("100.00"), "EUR"))
                .holdReason("AML_THRESHOLD")
                .heldAt(LocalDateTime.of(2026, 10, 14, 8, 0))
                .build();
        when(loadSuspendedPayments.search(any())).thenReturn(List.of(payment));

        // WHEN
        List<SuspendedPayment> actual = searchSuspendedPaymentsService.search(
                new SuspendedPaymentSearchQuery(null, null, null, null, null, null));

        // THEN
        assertThat(actual).containsExactly(payment);
        verify(loadSuspendedPayments).search(any());
        verifyNoMoreInteractions(loadSuspendedPayments);
    }

    @Test
    void should_load_with_the_criteria_built_from_the_raw_query() {
        // GIVEN
        SuspendedPaymentSearchQuery query = new SuspendedPaymentSearchQuery(" DE89370400440532013000 ", "aml_threshold",
                "pending", "2026-10-01T00:00:00", "2026-10-31T00:00:00", 5);
        ArgumentCaptor<SuspendedPaymentSearchCriteria> criteria = ArgumentCaptor.forClass(SuspendedPaymentSearchCriteria.class);

        // WHEN
        searchSuspendedPaymentsService.search(query);

        // THEN
        verify(loadSuspendedPayments).search(criteria.capture());
        assertThat(criteria.getValue())
                .returns(Optional.of("DE89370400440532013000"), SuspendedPaymentSearchCriteria::debtorIban)
                .returns(Optional.of(new CodeFilter("AML_THRESHOLD")), SuspendedPaymentSearchCriteria::holdReason)
                .returns(Optional.of(new CodeFilter("PENDING")), SuspendedPaymentSearchCriteria::reviewStatus)
                .returns(Optional.of(LocalDateTime.of(2026, 10, 1, 0, 0)), c -> c.heldFrom().value())
                .returns(Optional.of(LocalDateTime.of(2026, 10, 31, 0, 0)), c -> c.heldTo().value())
                .returns(new SearchLimit(5), SuspendedPaymentSearchCriteria::limit);
        verifyNoMoreInteractions(loadSuspendedPayments);
    }

    @Test
    void should_not_load_anything_when_the_limit_is_negative() {
        // GIVEN
        SuspendedPaymentSearchQuery query = new SuspendedPaymentSearchQuery(null, null, null, null, null, -1);

        // WHEN
        Throwable actual = org.assertj.core.api.Assertions.catchThrowable(() -> searchSuspendedPaymentsService.search(query));

        // THEN
        assertThat(actual).hasMessage("max-results cannot be negative");
        verify(loadSuspendedPayments, never()).search(any());
    }
}
