package fr.kevin.llps.payments.domain.outbound;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.outbound.port.out.LoadOutboundPayments;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOutboundPaymentDetailsServiceTest {

    private static final PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000401");

    @Mock
    private LoadOutboundPayments loadOutboundPayments;

    private GetOutboundPaymentDetailsService getOutboundPaymentDetailsService;

    @BeforeEach
    void createService() {
        getOutboundPaymentDetailsService = new GetOutboundPaymentDetailsService(loadOutboundPayments, new DetailsFeePolicy());
    }

    @Test
    void should_return_the_payment_with_the_details_fee() {
        // GIVEN (remapped from PaymentControllerWebTest.outboundDetails_computesFeeFromController: OUR 1000.00 -> 20.00)
        OutboundPayment payment = OutboundPayment.builder(id)
                .money(Money.of(new BigDecimal("1000.00"), "EUR"))
                .chargesType("OUR")
                .build();
        when(loadOutboundPayments.findById(id)).thenReturn(Optional.of(payment));

        // WHEN
        Optional<OutboundPaymentWithFee> actual = getOutboundPaymentDetailsService.getDetails(id);

        // THEN
        assertThat(actual).contains(new OutboundPaymentWithFee(payment, new BigDecimal("20.00")));
        verify(loadOutboundPayments).findById(id);
        verifyNoMoreInteractions(loadOutboundPayments);
    }

    @Test
    void should_return_empty_when_the_payment_does_not_exist() {
        // GIVEN (remapped from PaymentControllerWebTest.outboundDetails_missing_returns404, use-case part)
        PaymentId unknownId = PaymentId.parse("00000000-0000-0000-0000-000000000404");
        when(loadOutboundPayments.findById(unknownId)).thenReturn(Optional.empty());

        // WHEN
        Optional<OutboundPaymentWithFee> actual = getOutboundPaymentDetailsService.getDetails(unknownId);

        // THEN
        assertThat(actual).isEmpty();
    }

    @Test
    void should_propagate_the_fee_not_computable_error_of_an_our_payment_without_amount() {
        // GIVEN (BR-OUT-07: surfaces as the generic 500)
        OutboundPayment payment = OutboundPayment.builder(id).chargesType("OUR").build();
        when(loadOutboundPayments.findById(id)).thenReturn(Optional.of(payment));

        // WHEN
        Throwable actual = catchThrowable(() -> getOutboundPaymentDetailsService.getDetails(id));

        // THEN
        assertThat(actual).isInstanceOf(FeeNotComputableException.class);
    }
}
