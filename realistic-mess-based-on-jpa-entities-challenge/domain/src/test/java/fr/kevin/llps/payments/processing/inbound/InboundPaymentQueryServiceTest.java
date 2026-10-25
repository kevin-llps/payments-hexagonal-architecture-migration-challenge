package fr.kevin.llps.payments.processing.inbound;

import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentSearchQuery;
import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentView;
import fr.kevin.llps.payments.processing.inbound.port.out.InboundPaymentSearchRepository;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import fr.kevin.llps.payments.shared.domain.SearchLimit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundPaymentQueryServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-25T09:00:00Z"), ZoneOffset.UTC);
    private static final PaymentId ID = PaymentId.parse("00000000-0000-0000-0000-000000000301");

    @Mock
    private InboundPaymentSearchRepository repository;

    private InboundPaymentQueryService service;

    @BeforeEach
    void setUp() {
        service = new InboundPaymentQueryService(repository, CLOCK);
    }

    @Test
    void search_should_return_the_matching_payments_with_their_age_for_today() {
        // GIVEN
        InboundPaymentSearchQuery query = new InboundPaymentSearchQuery(null, "bnpa", null, null, null, null, 500);
        InboundPaymentSearchCriteria expectedCriteria = new InboundPaymentSearchCriteria(null, "BNPA", null, null,
                null, null, new SearchLimit(500));
        InboundPayment payment = payment(LocalDate.of(2026, 10, 22));
        when(repository.search(expectedCriteria)).thenReturn(List.of(payment));

        // WHEN
        List<InboundPaymentView> result = service.search(query);

        // THEN
        assertThat(result).containsExactly(new InboundPaymentView(payment, 3L));
    }

    @Test
    void get_should_return_the_payment_found_by_id_with_its_age_for_today() {
        // GIVEN
        InboundPayment payment = payment(LocalDate.of(2026, 10, 29));
        when(repository.findById(ID)).thenReturn(Optional.of(payment));

        // WHEN
        Optional<InboundPaymentView> result = service.get(ID);

        // THEN
        assertThat(result).contains(new InboundPaymentView(payment, -4L));
    }

    private static InboundPayment payment(LocalDate valueDate) {
        return new InboundPayment(ID, "CTR-1", Money.of(new BigDecimal("10.00"), "EUR"), "DE89", "ACME", "BNPAFRPP",
                valueDate, null, "RECEIVED", null);
    }
}
