package fr.kevin.llps.payments.domain.outbound;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.outbound.port.out.LoadOutboundPayments;
import fr.kevin.llps.payments.domain.outbound.query.OutboundPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.outbound.query.OutboundPaymentSearchQuery;
import fr.kevin.llps.payments.domain.sharedkernel.MalformedDateException;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchOutboundPaymentsServiceTest {

    @Mock
    private LoadOutboundPayments loadOutboundPayments;

    private SearchOutboundPaymentsService searchOutboundPaymentsService;

    @BeforeEach
    void createService() {
        searchOutboundPaymentsService = new SearchOutboundPaymentsService(loadOutboundPayments, new SearchFeePolicy());
    }

    @Test
    void should_apply_the_search_fee_to_every_loaded_payment_in_order() {
        // GIVEN (remapped from PaymentServiceUnitTest.outboundSearch_computesFeePerChargesType)
        OutboundPayment our = payment("00000000-0000-0000-0000-000000000411", "1000.00", "OUR");
        OutboundPayment sha = payment("00000000-0000-0000-0000-000000000412", "1000.00", "SHA");
        OutboundPayment ben = payment("00000000-0000-0000-0000-000000000413", "1000.00", "BEN");
        OutboundPayment nullAmount = payment("00000000-0000-0000-0000-000000000414", null, "SHA");
        when(loadOutboundPayments.search(any())).thenReturn(List.of(our, sha, ben, nullAmount));

        // WHEN
        List<OutboundPaymentWithFee> actual = searchOutboundPaymentsService.search(
                new OutboundPaymentSearchQuery(null, null, null, null, null, null, null));

        // THEN
        assertThat(actual).extracting(OutboundPaymentWithFee::payment).containsExactly(our, sha, ben, nullAmount);
        assertThat(actual.get(0).estimatedFee()).isEqualByComparingTo("10.00");
        assertThat(actual.get(1).estimatedFee()).isEqualByComparingTo("0.30");
        assertThat(actual.get(2).estimatedFee()).isEqualByComparingTo("0.00");
        assertThat(actual.get(3).estimatedFee()).isEqualByComparingTo("0.00");
    }

    @Test
    void should_load_with_the_criteria_built_from_the_raw_query() {
        // GIVEN
        OutboundPaymentSearchQuery query = new OutboundPaymentSearchQuery("DE89370400440532013000", "abna", "sha",
                "sent", "2026-10-01", "2026-10-31", 20);
        ArgumentCaptor<OutboundPaymentSearchCriteria> criteria = ArgumentCaptor.forClass(OutboundPaymentSearchCriteria.class);

        // WHEN
        searchOutboundPaymentsService.search(query);

        // THEN
        verify(loadOutboundPayments).search(criteria.capture());
        assertThat(criteria.getValue())
                .returns(Optional.of("ABNA"), OutboundPaymentSearchCriteria::destinationBicFragment)
                .returns(20, c -> c.limit().value());
        verifyNoMoreInteractions(loadOutboundPayments);
    }

    @Test
    void should_not_load_anything_when_a_date_bound_is_malformed() {
        // GIVEN
        OutboundPaymentSearchQuery query = new OutboundPaymentSearchQuery(null, null, null, null, "garbage", null, null);

        // WHEN
        Throwable actual = catchThrowable(() -> searchOutboundPaymentsService.search(query));

        // THEN
        assertThat(actual).isInstanceOf(MalformedDateException.class);
        verifyNoInteractions(loadOutboundPayments);
    }

    private static OutboundPayment payment(String id, String amount, String chargesType) {
        return OutboundPayment.builder(PaymentId.parse(id))
                .money(Money.of(amount == null ? null : new BigDecimal(amount), "EUR"))
                .chargesType(chargesType)
                .status("SENT")
                .build();
    }
}
