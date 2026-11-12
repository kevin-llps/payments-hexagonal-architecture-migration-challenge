package fr.kevin.llps.payments.domain.instant;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.instant.port.out.LoadInstantPayments;
import fr.kevin.llps.payments.domain.instant.query.InstantPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.instant.query.InstantPaymentSearchQuery;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchInstantPaymentsServiceTest {

    @Mock
    private LoadInstantPayments loadInstantPayments;

    @InjectMocks
    private SearchInstantPaymentsService searchInstantPaymentsService;

    @Test
    void should_return_the_loaded_payments_when_the_filters_are_valid() {
        // GIVEN (remapped from PaymentServiceUnitTest.instantSearch_validIbans_delegatesToRepository)
        InstantPayment payment = InstantPayment.builder(PaymentId.parse("00000000-0000-0000-0000-000000000101")).build();
        when(loadInstantPayments.search(any())).thenReturn(List.of(payment));

        // WHEN
        List<InstantPayment> actual = searchInstantPaymentsService.search(
                new InstantPaymentSearchQuery(null, null, null, null, null, null, null, null));

        // THEN
        assertThat(actual).hasSize(1).containsExactly(payment);
        verify(loadInstantPayments).search(any());
        verifyNoMoreInteractions(loadInstantPayments);
    }

    @Test
    void should_load_with_blank_iban_filters_ignored() {
        // GIVEN (remapped from PaymentServiceUnitTest.should_ignore_blank_iban_filters_and_delegate_to_repository)
        InstantPaymentSearchQuery query = new InstantPaymentSearchQuery(null, "   ", "", null, null, null, null, null);
        ArgumentCaptor<InstantPaymentSearchCriteria> criteria = ArgumentCaptor.forClass(InstantPaymentSearchCriteria.class);

        // WHEN
        List<InstantPayment> actual = searchInstantPaymentsService.search(query);

        // THEN
        verify(loadInstantPayments).search(criteria.capture());
        assertThat(actual).isEmpty();
        assertThat(criteria.getValue()).returns(Optional.empty(), InstantPaymentSearchCriteria::debtorIbanFragment);
        verifyNoMoreInteractions(loadInstantPayments);
    }

    @Test
    void should_load_with_a_spaced_iban_filter() {
        // GIVEN (remapped from PaymentServiceUnitTest.should_accept_iban_filter_containing_spaces_when_length_without_spaces_is_valid)
        InstantPaymentSearchQuery query = new InstantPaymentSearchQuery(null, "DE89 3704 0044 0532 0130 00", null, null,
                null, null, null, null);

        // WHEN
        List<InstantPayment> actual = searchInstantPaymentsService.search(query);

        // THEN
        assertThat(actual).isEmpty();
        verify(loadInstantPayments).search(any());
        verifyNoMoreInteractions(loadInstantPayments);
    }

    @Test
    void should_not_load_anything_when_an_iban_filter_is_invalid() {
        // GIVEN (remapped from PaymentServiceUnitTest.should_not_query_repository_when_creditor_iban_is_invalid / debtor-first case)
        InstantPaymentSearchQuery query = new InstantPaymentSearchQuery(null, "DE1", "FR1", null, null, null, null, null);

        // WHEN
        Throwable actual = catchThrowable(() -> searchInstantPaymentsService.search(query));

        // THEN
        assertThat(actual).isInstanceOf(InvalidIbanFilterException.class).hasMessage("Invalid debtor IBAN");
        verifyNoInteractions(loadInstantPayments);
    }
}
