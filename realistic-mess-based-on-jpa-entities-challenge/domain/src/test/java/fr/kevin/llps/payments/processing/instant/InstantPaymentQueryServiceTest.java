package fr.kevin.llps.payments.processing.instant;

import fr.kevin.llps.payments.processing.instant.port.in.InstantPaymentSearchQuery;
import fr.kevin.llps.payments.processing.instant.port.out.InstantPaymentSearchRepository;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import fr.kevin.llps.payments.shared.domain.SearchLimit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstantPaymentQueryServiceTest {

    private static final PaymentId ID = PaymentId.parse("00000000-0000-0000-0000-000000000101");

    @Mock
    private InstantPaymentSearchRepository repository;

    @InjectMocks
    private InstantPaymentQueryService service;

    @Test
    void search_should_return_the_payments_matching_the_criteria_built_from_the_query() {
        // GIVEN
        InstantPaymentSearchQuery query = new InstantPaymentSearchQuery("E2E-INST-0001", "de89370400440532013000",
                null, "completed", null, null, "2026-10-31", 10);
        InstantPaymentSearchCriteria expectedCriteria = new InstantPaymentSearchCriteria("E2E-INST-0001",
                "DE89370400440532013000", null, "COMPLETED", null, null, LocalDateTime.of(2026, 10, 31, 23, 59, 59),
                new SearchLimit(10));
        List<InstantPayment> payments = List.of(payment());
        when(repository.search(expectedCriteria)).thenReturn(payments);

        // WHEN
        List<InstantPayment> result = service.search(query);

        // THEN
        assertThat(result).isEqualTo(payments);
    }

    @Test
    void get_should_return_the_payment_found_by_id() {
        // GIVEN
        InstantPayment payment = payment();
        when(repository.findById(ID)).thenReturn(Optional.of(payment));

        // WHEN
        Optional<InstantPayment> result = service.get(ID);

        // THEN
        assertThat(result).contains(payment);
    }

    private static InstantPayment payment() {
        return new InstantPayment(ID, "E2E-INST-0001", null, Money.of(new BigDecimal("1500.00"), "EUR"), null, null,
                null, null, null, "COMPLETED", "SCT INST", null);
    }
}
