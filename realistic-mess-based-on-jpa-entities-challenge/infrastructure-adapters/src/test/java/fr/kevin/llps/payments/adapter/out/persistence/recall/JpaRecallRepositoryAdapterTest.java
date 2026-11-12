package fr.kevin.llps.payments.adapter.out.persistence.recall;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import fr.kevin.llps.payments.domain.recall.OriginalPaymentReference;
import fr.kevin.llps.payments.domain.recall.Recall;
import fr.kevin.llps.payments.domain.recall.query.RecallSearchCriteria;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=none", "spring.sql.init.mode=always"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaRecallRepositoryAdapter.class)
@Testcontainers
class JpaRecallRepositoryAdapterTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaRecallRepositoryAdapter adapter;
    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void clearTable() {
        em.getEntityManager().createNativeQuery("DELETE FROM recalls").executeUpdate();
        em.flush();
        em.clear();
    }

    @Test
    void should_always_return_at_most_fifty_recalls() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.recallSearch_alwaysUsesHardcodedLimitOf50; the request limit 5 never reaches the criteria)
        for (int i = 0; i < 51; i++) {
            persist(recall(String.format("00000000-0000-0000-0000-00000000%04d", 700 + i),
                    "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"), Date.valueOf("2026-10-18")));
        }
        RecallSearchCriteria criteria = RecallSearchCriteria.from(null, null, null, null, null, null, null);

        // WHEN
        List<Recall> results = adapter.search(criteria);

        // THEN
        assertThat(results).hasSize(50);
    }

    @Test
    void should_apply_every_filter() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.recallSearch_appliesFilters)
        persist(
                recall("00000000-0000-0000-0000-000000000751", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                        Date.valueOf("2026-10-18")),
                recall("00000000-0000-0000-0000-000000000752", "R-REJ", Timestamp.valueOf("2026-10-14 19:00:00"),
                        Date.valueOf("2026-10-16")));
        RecallSearchCriteria criteria = RecallSearchCriteria.from("r-cor", "00000000-0000-0000-0000-000000000401",
                "outbound", "ac04", "initiated", "2026-10-01T00:00:00", "2026-10-31T00:00:00");

        // WHEN
        List<Recall> results = adapter.search(criteria);

        // THEN
        assertThat(results).hasSize(1);
        assertThat(results.get(0).recallReference()).contains("RCL-T1");
    }

    @Test
    void should_find_a_recall_by_id() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.recallFindById_foundAndMissing, found case)
        persist(recall("00000000-0000-0000-0000-000000000753", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                Date.valueOf("2026-10-18")));

        // WHEN
        Optional<Recall> found = adapter.findById(PaymentId.parse("00000000-0000-0000-0000-000000000753"));

        // THEN
        assertThat(found).isPresent();
        assertThat(found.get().recallType()).contains("R-COR");
        assertThat(found.get().deadline()).contains(LocalDate.of(2026, 10, 18));
    }

    @Test
    void should_find_nothing_for_an_unknown_id() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.recallFindById_foundAndMissing, missing case)
        PaymentId unknownId = PaymentId.parse("11111111-1111-1111-1111-111111111111");

        // WHEN
        Optional<Recall> found = adapter.findById(unknownId);

        // THEN
        assertThat(found).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] initiatedFrom={0} initiatedTo={1} -> {2} rows")
    @CsvSource(value = {
            "NULL, 2026-10-16, 1",
            "NULL, 2026-10-17, 2",
            "2026-10-16, NULL, 1",
            "garbage, NULL, 2",
            "NULL, garbage, 2",
            "2026-10-16T12:00:00, 2026-10-16T12:00:00, 1"
    }, nullValues = "NULL")
    void should_treat_date_only_recall_bounds_as_start_of_day_and_ignore_unparsable_ones(String from, String to,
            int expectedCount) {
        // GIVEN (remapped from PaymentRepositoryUnitTest, same rows)
        persist(
                recall("00000000-0000-0000-0000-000000000761", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                        Date.valueOf("2026-10-18")),
                recall("00000000-0000-0000-0000-000000000762", "R-REJ", Timestamp.valueOf("2026-10-14 19:00:00"),
                        Date.valueOf("2026-10-16")));
        RecallSearchCriteria criteria = RecallSearchCriteria.from(null, null, null, null, null, from, to);

        // WHEN
        List<Recall> results = adapter.search(criteria);

        // THEN
        assertThat(results).hasSize(expectedCount);
    }

    @Test
    void should_order_recalls_by_initiated_at_descending() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.should_order_recalls_by_initiated_at_descending)
        persist(
                recall("00000000-0000-0000-0000-000000000763", "R-REJ", Timestamp.valueOf("2026-10-14 19:00:00"),
                        Date.valueOf("2026-10-16")),
                recall("00000000-0000-0000-0000-000000000764", "R-UPD", Timestamp.valueOf("2026-10-16 20:00:00"),
                        Date.valueOf("2026-10-19")),
                recall("00000000-0000-0000-0000-000000000765", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                        Date.valueOf("2026-10-18")));
        RecallSearchCriteria criteria = RecallSearchCriteria.from(null, null, null, null, null, null, null);

        // WHEN
        List<Recall> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().extracting(r -> r.recallType().orElse(null)).containsExactly("R-UPD", "R-COR", "R-REJ");
    }

    @Test
    void should_accept_recall_without_amount_and_currency() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.should_accept_recall_without_amount_and_currency)
        fr.kevin.llps.payments.entity.Recall recall = recall("00000000-0000-0000-0000-000000000685", "R-COR",
                Timestamp.valueOf("2026-10-16 12:00:00"), Date.valueOf("2026-10-18"));
        recall.amount = null;
        recall.currency = null;
        persist(recall);

        // WHEN
        Optional<Recall> found = adapter.findById(PaymentId.parse("00000000-0000-0000-0000-000000000685"));

        // THEN
        assertThat(found).isPresent();
        assertThat(found.get().money().amount()).isEmpty();
        assertThat(found.get().money().currency()).isEmpty();
    }

    @Test
    void should_replace_every_column_when_saving_an_existing_id() {
        // GIVEN (BR-ING-03 full-replace upsert)
        persist(recall("00000000-0000-0000-0000-000000000766", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                Date.valueOf("2026-10-18")));
        LocalDateTime consumerTime = LocalDateTime.of(2026, 11, 12, 11, 0);
        Recall replacement = Recall.builder(PaymentId.parse("00000000-0000-0000-0000-000000000766"))
                .recallType("R-XXX")
                .originalPayment(OriginalPaymentReference.of(PaymentId.parse("99999999-9999-9999-9999-999999999999"), "CHEQUE"))
                .money(Money.of(BigDecimal.ZERO, ""))
                .status("unknown")
                .createdAt(consumerTime)
                .updatedAt(consumerTime)
                .build();

        // WHEN
        adapter.save(replacement);
        em.flush();
        em.clear();

        // THEN
        fr.kevin.llps.payments.entity.Recall stored =
                em.find(fr.kevin.llps.payments.entity.Recall.class, UUID.fromString("00000000-0000-0000-0000-000000000766"));
        assertThat(stored.recallType).isEqualTo("R-XXX");
        assertThat(stored.originalPaymentType).isEqualTo("CHEQUE");
        assertThat(stored.recallReference).isNull();
        assertThat(stored.deadline).isNull();
        assertThat(stored.amount).isEqualByComparingTo("0");
        assertThat(stored.updatedAt).isEqualTo(Timestamp.valueOf("2026-11-12 11:00:00"));
    }

    private void persist(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.flush();
        em.clear();
    }

    private fr.kevin.llps.payments.entity.Recall recall(String id, String recallType, Timestamp initiatedAt, Date deadline) {
        fr.kevin.llps.payments.entity.Recall recall = new fr.kevin.llps.payments.entity.Recall();
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
