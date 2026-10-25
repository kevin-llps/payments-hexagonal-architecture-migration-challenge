package fr.kevin.llps.payments.processing.outbound;

import fr.kevin.llps.payments.processing.outbound.port.in.OutboundPaymentSearchQuery;
import fr.kevin.llps.payments.processing.outbound.port.in.PricedOutboundPayment;
import fr.kevin.llps.payments.processing.outbound.port.out.OutboundPaymentSearchRepository;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import fr.kevin.llps.payments.shared.domain.SearchLimit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static fr.kevin.llps.payments.processing.outbound.SearchFeeEstimateTest.payment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundPaymentQueryServiceTest {

    private static final BigDecimal SEARCH_TARIFF_FEE = new BigDecimal("1.11");
    private static final BigDecimal DETAIL_TARIFF_FEE = new BigDecimal("2.22");

    @Mock
    private OutboundPaymentSearchRepository repository;

    private OutboundPaymentQueryService service;

    @BeforeEach
    void setUp() {
        service = new OutboundPaymentQueryService(repository, payment -> SEARCH_TARIFF_FEE, payment -> DETAIL_TARIFF_FEE);
    }

    @Test
    void search_should_price_the_matching_payments_with_the_search_tariff() {
        // GIVEN
        OutboundPaymentSearchQuery query = new OutboundPaymentSearchQuery(null, "abna", null, null, null, null, null);
        OutboundPaymentSearchCriteria expectedCriteria = new OutboundPaymentSearchCriteria(null, "ABNA", null, null,
                null, null, new SearchLimit(50));
        OutboundPayment payment = payment("1000.00", "OUR");
        when(repository.search(expectedCriteria)).thenReturn(List.of(payment));

        // WHEN
        List<PricedOutboundPayment> result = service.search(query);

        // THEN
        assertThat(result).containsExactly(new PricedOutboundPayment(payment, SEARCH_TARIFF_FEE));
    }

    @Test
    void get_should_price_the_payment_found_by_id_with_the_detail_tariff() {
        // GIVEN
        OutboundPayment payment = payment("1000.00", "OUR");
        PaymentId id = payment.id();
        when(repository.findById(id)).thenReturn(Optional.of(payment));

        // WHEN
        Optional<PricedOutboundPayment> result = service.get(id);

        // THEN
        assertThat(result).contains(new PricedOutboundPayment(payment, DETAIL_TARIFF_FEE));
    }
}
