package fr.kevin.llps.payments.processing.instant.adapter.out.persistence;

import fr.kevin.llps.payments.JpaAdapterTestSupport;
import fr.kevin.llps.payments.processing.instant.InstantPaymentSearchCriteria;
import fr.kevin.llps.payments.processing.instant.adapter.out.persistence.entity.InstantPayment;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import fr.kevin.llps.payments.shared.domain.SearchLimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JpaInstantPaymentAdapter} against PostgreSQL; carried over from the legacy PaymentRepositoryUnitTest.
 */
@Import(JpaInstantPaymentAdapter.class)
class JpaInstantPaymentAdapterTest extends JpaAdapterTestSupport {

    @Autowired
    private JpaInstantPaymentAdapter instantRepository;

    // executionDatetime rendering moved to PaymentControllerWebTest.instantSearch_formatsExecutionDatetime
    @Test
    void instantSearch_appliesAllFilters_andFormatsExecutionDatetime() {
        InstantPayment match = instant("00000000-0000-0000-0000-000000000601", "E2E-T1", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR");
        InstantPayment other = instant("00000000-0000-0000-0000-000000000602", "E2E-T2", "PENDING",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-16 10:00:00"), "EUR");
        persist(match, other);

        var results = instantRepository.search(InstantPaymentSearchCriteria.from("E2E-T1",
                "de89370400440532013000", "fr7630006000011234567890189", "completed", "eur", "2026-10-01T00:00:00",
                "2026-10-31", 10));

        assertThat(results).hasSize(1);
        var payment = results.get(0);
        assertThat(payment.id()).isEqualTo(PaymentId.parse("00000000-0000-0000-0000-000000000601"));
        assertThat(payment.endToEndId()).isEqualTo("E2E-T1");
        assertThat(payment.amount().amount()).isEqualByComparingTo("1500.00");
        assertThat(payment.executionDatetime()).isEqualTo(LocalDateTime.of(2026, 10, 15, 9, 15, 0));
        assertThat(payment.scheme()).isEqualTo("SCT INST");
    }

    @Test
    void instantSearch_ordersByExecutionDatetimeDescending() {
        persist(
                instant("00000000-0000-0000-0000-000000000603", "E2E-OLD", "PENDING",
                        "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-10 08:00:00"), "EUR"),
                instant("00000000-0000-0000-0000-000000000604", "E2E-NEW", "COMPLETED",
                        "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-16 08:00:00"), "EUR"));

        var results = instantRepository.search(instantCriteria(null, null, null, null));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).endToEndId()).isEqualTo("E2E-NEW");
        assertThat(results.get(1).endToEndId()).isEqualTo("E2E-OLD");
    }

    // Substrings shorter than 15 chars are rejected as IBAN filters by the criteria factory (BR-07), so this adapter
    // test builds the criteria directly with the trimmed, upper-cased values the factory produces.
    @Test
    void instantSearch_ibanFilters_matchAnySubstringIgnoringCase() {
        persist(instant("00000000-0000-0000-0000-000000000606", "E2E-S1", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));

        assertThat(instantRepository.search(new InstantPaymentSearchCriteria(null, "0532013", "6000011234", null, null,
                null, null, new SearchLimit(50)))).extracting(r -> r.endToEndId()).containsExactly("E2E-S1");
    }

    @Test
    void instantSearch_dateOnlyBounds_coverTheWholeDay() {
        persist(
                instant("00000000-0000-0000-0000-000000000607", "E2E-MIDNIGHT", "COMPLETED",
                        "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 00:00:00"), "EUR"),
                instant("00000000-0000-0000-0000-000000000608", "E2E-LATE", "COMPLETED",
                        "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 23:30:00"), "EUR"),
                instant("00000000-0000-0000-0000-000000000609", "E2E-NEXT", "COMPLETED",
                        "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-16 00:00:00"), "EUR"));

        assertThat(instantRepository.search(instantCriteria("2026-10-15", "2026-10-15", null, null)))
                .extracting(r -> r.endToEndId())
                .containsExactly("E2E-LATE", "E2E-MIDNIGHT");
    }

    @Test
    void instantSearch_unparseableExecutionBounds_areIgnored() {
        persist(instant("00000000-0000-0000-0000-000000000610", "E2E-ANY", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));

        assertThat(instantRepository.search(instantCriteria("yesterday", "2026/10/01", null, null)))
                .extracting(r -> r.endToEndId()).containsExactly("E2E-ANY");
    }

    @Test
    void instantSearch_withoutLimit_returnsAtMost50Rows() {
        for (int i = 0; i < 51; i++) {
            persist(instant(String.format("00000000-0000-0000-0000-00000001%04d", i), "E2E-L" + i, "COMPLETED",
                    "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));
        }

        assertThat(instantRepository.search(instantCriteria(null, null, null, null))).hasSize(50);
    }

    // executionDatetime rendering moved to PaymentControllerWebTest.instantDetails_formatsExecutionDatetime
    @Test
    void instantFindById_foundAndMissing() {
        persist(instant("00000000-0000-0000-0000-000000000605", "E2E-F1", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));

        var found = instantRepository.findById(PaymentId.parse("00000000-0000-0000-0000-000000000605")).orElseThrow();
        assertThat(found.endToEndId()).isEqualTo("E2E-F1");
        assertThat(found.executionDatetime()).isEqualTo(LocalDateTime.of(2026, 10, 15, 9, 15, 0));
        assertThat(instantRepository.findById(PaymentId.parse("11111111-1111-1111-1111-111111111111"))).isEmpty();
    }

    private InstantPaymentSearchCriteria instantCriteria(String executionFrom, String executionTo, String status,
                                                         Integer limit) {
        return InstantPaymentSearchCriteria.from(null, null, null, status, null, executionFrom, executionTo, limit);
    }

    private InstantPayment instant(String id, String endToEndId, String status, String debtorIban,
            String creditorIban, Timestamp executionDatetime, String currency) {
        InstantPayment payment = new InstantPayment();
        payment.id = UUID.fromString(id);
        payment.endToEndId = endToEndId;
        payment.uetr = "uetr-" + endToEndId;
        payment.amount = new BigDecimal("1500.00");
        payment.currency = currency;
        payment.debtorIban = debtorIban;
        payment.debtorName = "ACME";
        payment.creditorIban = creditorIban;
        payment.creditorName = "Dupont";
        payment.executionDatetime = executionDatetime;
        payment.status = status;
        payment.scheme = "SCT INST";
        payment.createdAt = executionDatetime;
        payment.updatedAt = executionDatetime;
        return payment;
    }
}
