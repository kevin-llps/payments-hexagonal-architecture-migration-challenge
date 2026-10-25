package fr.kevin.llps.payments.compliancehold.adapter.out.persistence;

import fr.kevin.llps.payments.JpaAdapterTestSupport;
import fr.kevin.llps.payments.compliancehold.SuspendedPaymentSearchCriteria;
import fr.kevin.llps.payments.compliancehold.adapter.out.persistence.entity.SuspendedPayment;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JpaSuspendedPaymentAdapter} against PostgreSQL; carried over from the legacy PaymentRepositoryUnitTest.
 */
@Import(JpaSuspendedPaymentAdapter.class)
class JpaSuspendedPaymentAdapterTest extends JpaAdapterTestSupport {

    @Autowired
    private JpaSuspendedPaymentAdapter suspendedRepository;

    @Test
    void suspendedSearch_appliesParameterizedFilters() {
        persist(
                suspended("00000000-0000-0000-0000-000000000611", "DE89370400440532013000", "AML_THRESHOLD",
                        "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null),
                suspended("00000000-0000-0000-0000-000000000612", "DE89370400440532013000", "SANCTIONS_SCREENING",
                        "PENDING", Timestamp.valueOf("2026-10-14 09:00:00"), null),
                suspended("00000000-0000-0000-0000-000000000613", "DE89370400440532013000", "AML_THRESHOLD",
                        "RELEASED", Timestamp.valueOf("2026-10-13 08:00:00"), Timestamp.valueOf("2026-10-13 15:00:00")));

        var results = suspendedRepository.search(SuspendedPaymentSearchCriteria.from("DE89370400440532013000",
                "aml_threshold", "pending", "2026-10-01T00:00:00", "2026-10-31T00:00:00", 5));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(PaymentId.parse("00000000-0000-0000-0000-000000000611"));
        assertThat(results.get(0).holdReason()).isEqualTo("AML_THRESHOLD");
        assertThat(results.get(0).heldAt()).isEqualTo(LocalDateTime.of(2026, 10, 14, 8, 0, 0));
    }

    @Test
    void suspendedSearch_debtorIbanFilter_isTrimmedExactAndCaseSensitive() {
        persist(suspended("00000000-0000-0000-0000-000000000615", "DE89370400440532013000", "AML_THRESHOLD",
                "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null));

        assertThat(suspendedRepository.search(suspendedByDebtorIban("  DE89370400440532013000 "))).hasSize(1);
        assertThat(suspendedRepository.search(suspendedByDebtorIban("DE89"))).isEmpty();
        assertThat(suspendedRepository.search(suspendedByDebtorIban("de89370400440532013000"))).isEmpty();
    }

    @Test
    void suspendedSearch_ordersByHeldAtDescending() {
        persist(
                suspended("00000000-0000-0000-0000-000000000616", "DE89370400440532013000", "AML_THRESHOLD",
                        "PENDING", Timestamp.valueOf("2026-10-12 08:00:00"), null),
                suspended("00000000-0000-0000-0000-000000000617", "DE89370400440532013000", "AML_THRESHOLD",
                        "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null));

        assertThat(suspendedRepository.search(SuspendedPaymentSearchCriteria.from(null, null, null, null, null, null)))
                .extracting(p -> p.id().value().toString())
                .containsExactly("00000000-0000-0000-0000-000000000617", "00000000-0000-0000-0000-000000000616");
    }

    @Test
    void suspendedSearch_heldBoundNotInLegacyTimestampFormat_returnsNoRows() {
        persist(suspended("00000000-0000-0000-0000-000000000618", "DE89370400440532013000", "AML_THRESHOLD",
                "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null));

        assertThat(suspendedRepository.search(SuspendedPaymentSearchCriteria.from(null, null, null, "2026-10-01", null,
                null))).isEmpty();
    }

    @Test
    void suspendedFindById_foundAndMissing() {
        persist(suspended("00000000-0000-0000-0000-000000000614", "DE89370400440532013000", "AML_THRESHOLD",
                "RELEASED", Timestamp.valueOf("2026-10-14 08:00:00"), Timestamp.valueOf("2026-10-13 15:45:00")));

        var found = suspendedRepository.findById(PaymentId.parse("00000000-0000-0000-0000-000000000614")).orElseThrow();
        assertThat(found.reviewStatus()).isEqualTo("RELEASED");
        assertThat(found.releasedAt()).isEqualTo(LocalDateTime.of(2026, 10, 13, 15, 45, 0));
        assertThat(suspendedRepository.findById(PaymentId.parse("11111111-1111-1111-1111-111111111111"))).isEmpty();
    }

    private SuspendedPaymentSearchCriteria suspendedByDebtorIban(String debtorIban) {
        return SuspendedPaymentSearchCriteria.from(debtorIban, null, null, null, null, null);
    }

    private SuspendedPayment suspended(String id, String debtorIban, String holdReason, String reviewStatus,
            Timestamp heldAt, Timestamp releasedAt) {
        SuspendedPayment payment = new SuspendedPayment();
        payment.id = UUID.fromString(id);
        payment.amount = new BigDecimal("12000.00");
        payment.currency = "EUR";
        payment.debtorIban = debtorIban;
        payment.debtorName = "ACME";
        payment.creditorIban = "FR7630006000011234567890189";
        payment.creditorName = "Dupont";
        payment.holdReason = holdReason;
        payment.complianceCaseRef = "CASE-" + id.substring(id.length() - 3);
        payment.reviewStatus = reviewStatus;
        payment.heldAt = heldAt;
        payment.releasedAt = releasedAt;
        payment.createdAt = heldAt;
        payment.updatedAt = heldAt;
        return payment;
    }
}
