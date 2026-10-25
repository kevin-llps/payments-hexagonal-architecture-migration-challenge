package fr.kevin.llps.payments.recall.adapter.out.persistence;

import fr.kevin.llps.payments.JpaAdapterTestSupport;
import fr.kevin.llps.payments.recall.RecallSearchCriteria;
import fr.kevin.llps.payments.recall.adapter.out.persistence.entity.Recall;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JpaRecallAdapter} against PostgreSQL; carried over from the legacy PaymentRepositoryUnitTest.
 */
@Import(JpaRecallAdapter.class)
class JpaRecallAdapterTest extends JpaAdapterTestSupport {

    @Autowired
    private JpaRecallAdapter recallRepository;

    @Test
    void recallSearch_alwaysUsesHardcodedLimitOf50() {
        for (int i = 0; i < 51; i++) {
            persist(recall(String.format("00000000-0000-0000-0000-00000000%04d", 700 + i),
                    "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"), Date.valueOf("2026-10-18")));
        }

        assertThat(recallRepository.search(recallCriteria(null, null, null, null, null, null, null, 5))).hasSize(50);
    }

    @Test
    void recallSearch_appliesFilters() {
        persist(
                recall("00000000-0000-0000-0000-000000000751", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                        Date.valueOf("2026-10-18")),
                recall("00000000-0000-0000-0000-000000000752", "R-REJ", Timestamp.valueOf("2026-10-14 19:00:00"),
                        Date.valueOf("2026-10-16")));

        var results = recallRepository.search(recallCriteria("r-cor", "00000000-0000-0000-0000-000000000401",
                "outbound", "ac04", "initiated", "2026-10-01T00:00:00", "2026-10-31T00:00:00", null));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).recallReference()).isEqualTo("RCL-T1");
    }

    @Test
    void recallSearch_dateOnlyInitiatedTo_excludesThatDay() {
        persist(recall("00000000-0000-0000-0000-000000000754", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                Date.valueOf("2026-10-18")));

        assertThat(recallRepository.search(recallInitiatedTo("2026-10-16"))).isEmpty();
        assertThat(recallRepository.search(recallInitiatedTo("2026-10-17"))).hasSize(1);
    }

    @Test
    void recallSearch_unparseableInitiatedBounds_areIgnored() {
        persist(recall("00000000-0000-0000-0000-000000000755", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                Date.valueOf("2026-10-18")));

        assertThat(recallRepository.search(recallCriteria(null, null, null, null, null, "last week", "16/10/2026",
                null))).hasSize(1);
    }

    @Test
    void recallSearch_ordersByInitiatedAtDescending() {
        persist(
                recall("00000000-0000-0000-0000-000000000756", "R-REJ", Timestamp.valueOf("2026-10-14 19:00:00"),
                        Date.valueOf("2026-10-16")),
                recall("00000000-0000-0000-0000-000000000757", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                        Date.valueOf("2026-10-18")));

        assertThat(recallRepository.search(recallCriteria(null, null, null, null, null, null, null, null)))
                .extracting(r -> r.recallType())
                .containsExactly("R-COR", "R-REJ");
    }

    @Test
    void recallFindById_foundAndMissing() {
        persist(recall("00000000-0000-0000-0000-000000000753", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                Date.valueOf("2026-10-18")));

        var found = recallRepository.findById(PaymentId.parse("00000000-0000-0000-0000-000000000753")).orElseThrow();
        assertThat(found.recallType()).isEqualTo("R-COR");
        assertThat(found.deadline()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(recallRepository.findById(PaymentId.parse("11111111-1111-1111-1111-111111111111"))).isEmpty();
    }

    private RecallSearchCriteria recallInitiatedTo(String initiatedTo) {
        return recallCriteria(null, null, null, null, null, null, initiatedTo, null);
    }

    private RecallSearchCriteria recallCriteria(String recallType, String originalPaymentId, String originalPaymentType,
                                                String reasonCode, String status, String initiatedFrom,
                                                String initiatedTo, Integer limit) {
        return RecallSearchCriteria.from(recallType, originalPaymentId, originalPaymentType, reasonCode, status,
                initiatedFrom, initiatedTo, limit);
    }

    private Recall recall(String id, String recallType, Timestamp initiatedAt, Date deadline) {
        Recall recall = new Recall();
        recall.id = UUID.fromString(id);
        recall.recallReference = "RCL-T1";
        recall.recallType = recallType;
        recall.originalPaymentId = UUID.fromString("00000000-0000-0000-0000-000000000401");
        recall.originalPaymentType = "OUTBOUND";
        recall.reasonCode = "AC04";
        recall.amount = new BigDecimal("300.00");
        recall.currency = "EUR";
        recall.initiatedAt = initiatedAt;
        recall.deadline = deadline;
        recall.status = "INITIATED";
        recall.createdAt = initiatedAt;
        recall.updatedAt = initiatedAt;
        return recall;
    }
}
