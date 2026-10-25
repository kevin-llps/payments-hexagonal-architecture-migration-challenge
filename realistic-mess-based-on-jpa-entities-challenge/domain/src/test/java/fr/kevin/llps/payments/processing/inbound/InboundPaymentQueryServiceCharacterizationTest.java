package fr.kevin.llps.payments.processing.inbound;

import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentSearchQuery;
import fr.kevin.llps.payments.processing.inbound.port.out.InboundPaymentSearchRepository;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Characterization tests of the legacy service behavior (Phase 2), carried over from the legacy
 * PaymentServiceUnitTest and re-targeted to {@link InboundPaymentQueryService}.
 */
@ExtendWith(MockitoExtension.class)
class InboundPaymentQueryServiceCharacterizationTest {

    @Mock
    private InboundPaymentSearchRepository inboundRepository;

    // the real system clock, as the legacy service used LocalDate.now()
    private InboundPaymentQueryService inboundQueries;

    @BeforeEach
    void wireInboundQueries() {
        inboundQueries = new InboundPaymentQueryService(inboundRepository, Clock.systemDefaultZone());
    }

    @Test
    void inboundSearch_delegatesToRepository() {
        when(inboundRepository.search(any())).thenReturn(List.of(inbound("00000000-0000-0000-0000-000000000301", null)));
        assertThat(inboundQueries.search(new InboundPaymentSearchQuery(null, null, null, null, null, null, null)))
                .hasSize(1);
    }

    // moved from PaymentControllerWebTest: the age shown in search results is computed by the query service
    @Test
    void inboundSearch_computesAgeInDays() {
        when(inboundRepository.search(any())).thenReturn(List.of(
                inbound("00000000-0000-0000-0000-000000000301", LocalDate.now().minusDays(2)),
                inbound("00000000-0000-0000-0000-000000000302", LocalDate.now().plusDays(5)),
                inbound("00000000-0000-0000-0000-000000000303", null)));

        assertThat(inboundQueries.search(new InboundPaymentSearchQuery(null, null, null, null, null, null, null)))
                .extracting(view -> view.ageInDays())
                .containsExactly(2L, -5L, null);
    }

    @Test
    void inboundGetById_computesAgeInDays() {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000301");
        when(inboundRepository.findById(id)).thenReturn(Optional.of(inbound(id.value().toString(),
                LocalDate.now().minusDays(3))));

        assertThat(inboundQueries.get(id).orElseThrow().ageInDays()).isEqualTo(3L);
    }

    @Test
    void inboundGetById_futureValueDate_hasNegativeAgeInDays() {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000302");
        when(inboundRepository.findById(id)).thenReturn(Optional.of(inbound(id.value().toString(),
                LocalDate.now().plusDays(4))));

        assertThat(inboundQueries.get(id).orElseThrow().ageInDays()).isEqualTo(-4L);
    }

    @Test
    void inboundGetById_withoutValueDate_leavesAgeInDaysNull() {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000303");
        when(inboundRepository.findById(id)).thenReturn(Optional.of(inbound(id.value().toString(), null)));

        assertThat(inboundQueries.get(id).orElseThrow().ageInDays()).isNull();
    }

    @Test
    void inboundGetById_missing_returnsNull() {
        PaymentId missing = PaymentId.parse("00000000-0000-0000-0000-000000000399");
        when(inboundRepository.findById(missing)).thenReturn(Optional.empty());
        assertThat(inboundQueries.get(missing)).isEmpty();
    }

    private static InboundPayment inbound(String id, LocalDate valueDate) {
        return new InboundPayment(PaymentId.parse(id), null, Money.of(new BigDecimal("1.00"), "EUR"), null, null, null,
                valueDate, null, null, null);
    }
}
