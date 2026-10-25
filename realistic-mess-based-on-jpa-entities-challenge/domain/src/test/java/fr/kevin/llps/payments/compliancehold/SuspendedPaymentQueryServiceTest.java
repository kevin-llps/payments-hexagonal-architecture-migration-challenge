package fr.kevin.llps.payments.compliancehold;

import fr.kevin.llps.payments.compliancehold.port.in.SuspendedPaymentSearchQuery;
import fr.kevin.llps.payments.compliancehold.port.out.SuspendedPaymentSearchRepository;
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
class SuspendedPaymentQueryServiceTest {

    private static final PaymentId ID = PaymentId.parse("00000000-0000-0000-0000-000000000201");

    @Mock
    private SuspendedPaymentSearchRepository repository;

    @InjectMocks
    private SuspendedPaymentQueryService service;

    @Test
    void search_should_return_the_payments_matching_the_criteria_built_from_the_query() {
        // GIVEN
        SuspendedPaymentSearchQuery query = new SuspendedPaymentSearchQuery(" DE89370400440532013000", "aml_threshold",
                null, "2026-10-01T00:00:00", null, 5);
        SuspendedPaymentSearchCriteria expectedCriteria = new SuspendedPaymentSearchCriteria("DE89370400440532013000",
                "AML_THRESHOLD", null, LocalDateTime.of(2026, 10, 1, 0, 0), null, false, new SearchLimit(5));
        List<SuspendedPayment> payments = List.of(payment());
        when(repository.search(expectedCriteria)).thenReturn(payments);

        // WHEN
        List<SuspendedPayment> result = service.search(query);

        // THEN
        assertThat(result).isEqualTo(payments);
    }

    @Test
    void get_should_return_the_payment_found_by_id() {
        // GIVEN
        SuspendedPayment payment = payment();
        when(repository.findById(ID)).thenReturn(Optional.of(payment));

        // WHEN
        Optional<SuspendedPayment> result = service.get(ID);

        // THEN
        assertThat(result).contains(payment);
    }

    private static SuspendedPayment payment() {
        return new SuspendedPayment(ID, Money.of(new BigDecimal("12000.00"), "EUR"), "DE89370400440532013000", "ACME",
                null, null, "AML_THRESHOLD", "CASE-1", "PENDING", null, null, null);
    }
}
