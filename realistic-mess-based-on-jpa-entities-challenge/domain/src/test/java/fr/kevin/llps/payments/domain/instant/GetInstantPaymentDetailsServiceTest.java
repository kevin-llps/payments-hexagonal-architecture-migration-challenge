package fr.kevin.llps.payments.domain.instant;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.instant.port.out.LoadInstantPayments;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetInstantPaymentDetailsServiceTest {

    @Mock
    private LoadInstantPayments loadInstantPayments;

    @InjectMocks
    private GetInstantPaymentDetailsService getInstantPaymentDetailsService;

    @Test
    void should_return_the_payment_when_it_exists() {
        // GIVEN (remapped from PaymentServiceUnitTest.instantGetById_delegatesToRepository, found case; UUID id)
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000101");
        InstantPayment payment = InstantPayment.builder(id).build();
        when(loadInstantPayments.findById(id)).thenReturn(Optional.of(payment));

        // WHEN
        Optional<InstantPayment> actual = getInstantPaymentDetailsService.getDetails(id);

        // THEN
        assertThat(actual).contains(payment);
        verify(loadInstantPayments).findById(id);
        verifyNoMoreInteractions(loadInstantPayments);
    }

    @Test
    void should_return_empty_when_the_payment_does_not_exist() {
        // GIVEN (remapped from PaymentServiceUnitTest.instantGetById_delegatesToRepository, missing case; unknown UUID)
        PaymentId id = PaymentId.parse("11111111-1111-1111-1111-111111111111");
        when(loadInstantPayments.findById(id)).thenReturn(Optional.empty());

        // WHEN
        Optional<InstantPayment> actual = getInstantPaymentDetailsService.getDetails(id);

        // THEN
        assertThat(actual).isEmpty();
        verify(loadInstantPayments).findById(id);
        verifyNoMoreInteractions(loadInstantPayments);
    }
}
