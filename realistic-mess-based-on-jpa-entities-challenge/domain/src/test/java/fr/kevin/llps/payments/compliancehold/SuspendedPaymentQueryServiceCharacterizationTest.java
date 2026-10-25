package fr.kevin.llps.payments.compliancehold;

import fr.kevin.llps.payments.compliancehold.port.in.SuspendedPaymentSearchQuery;
import fr.kevin.llps.payments.compliancehold.port.out.SuspendedPaymentSearchRepository;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Characterization tests of the legacy service behavior (Phase 2), carried over from the legacy
 * PaymentServiceUnitTest and re-targeted to {@link SuspendedPaymentQueryService}.
 */
@ExtendWith(MockitoExtension.class)
class SuspendedPaymentQueryServiceCharacterizationTest {

    @Mock
    private SuspendedPaymentSearchRepository suspendedRepository;
    @InjectMocks
    private SuspendedPaymentQueryService suspendedQueries;

    // suspendedSearch_mapsAllFields moved to PaymentControllerWebTest (rendering is a REST adapter concern)
    @Test
    void suspendedSearch_returnsRepositoryRows() {
        SuspendedPayment payment = suspended("00000000-0000-0000-0000-000000000201");
        when(suspendedRepository.search(any())).thenReturn(List.of(payment));

        assertThat(suspendedQueries.search(new SuspendedPaymentSearchQuery(null, null, null, null, null, null)))
                .containsExactly(payment);
    }

    // releasedAt rendering moved to PaymentControllerWebTest.suspendedDetails_formatsReleasedAt
    @Test
    void suspendedGetById_foundAndMissing() {
        PaymentId found = PaymentId.parse("00000000-0000-0000-0000-000000000202");
        PaymentId missing = PaymentId.parse("00000000-0000-0000-0000-000000000299");
        when(suspendedRepository.findById(found)).thenReturn(Optional.of(suspended(found.value().toString())));
        when(suspendedRepository.findById(missing)).thenReturn(Optional.empty());

        assertThat(suspendedQueries.get(found)).isPresent();
        assertThat(suspendedQueries.get(missing)).isEmpty();
    }

    private static SuspendedPayment suspended(String id) {
        return new SuspendedPayment(PaymentId.parse(id), Money.of(new BigDecimal("100.00"), "EUR"), null, null, null,
                null, "AML_THRESHOLD", null, "RELEASED", null, null, null);
    }
}
