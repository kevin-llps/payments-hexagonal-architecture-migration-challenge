package fr.kevin.llps.payments.domain.suspended;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.suspended.port.out.LoadSuspendedPayments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSuspendedPaymentDetailsServiceTest {

    @Mock
    private LoadSuspendedPayments loadSuspendedPayments;

    @InjectMocks
    private GetSuspendedPaymentDetailsService getSuspendedPaymentDetailsService;

    @Test
    void should_return_the_payment_when_it_exists() {
        // GIVEN (remapped from PaymentServiceUnitTest.suspendedGetById_foundAndMissing, found case)
        PaymentId id = PaymentId.of(UUID.fromString("00000000-0000-0000-0000-000000000202"));
        SuspendedPayment payment = SuspendedPayment.builder(id)
                .reviewStatus("RELEASED")
                .releasedAt(LocalDateTime.of(2026, 10, 13, 15, 45))
                .build();
        when(loadSuspendedPayments.findById(id)).thenReturn(Optional.of(payment));

        // WHEN
        Optional<SuspendedPayment> actual = getSuspendedPaymentDetailsService.getDetails(id);

        // THEN
        assertThat(actual).contains(payment);
        verify(loadSuspendedPayments).findById(id);
        verifyNoMoreInteractions(loadSuspendedPayments);
    }

    @Test
    void should_return_empty_when_the_payment_does_not_exist() {
        // GIVEN (remapped from PaymentServiceUnitTest.suspendedGetById_foundAndMissing, missing case)
        PaymentId id = PaymentId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        when(loadSuspendedPayments.findById(id)).thenReturn(Optional.empty());

        // WHEN
        Optional<SuspendedPayment> actual = getSuspendedPaymentDetailsService.getDetails(id);

        // THEN
        assertThat(actual).isEmpty();
        verify(loadSuspendedPayments).findById(id);
        verifyNoMoreInteractions(loadSuspendedPayments);
    }
}
