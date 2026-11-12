package fr.kevin.llps.payments.adapter.out.persistence.suspended;

import java.math.BigDecimal;
import java.sql.Timestamp;
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

import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.suspended.SuspendedPayment;
import fr.kevin.llps.payments.domain.suspended.query.SuspendedPaymentSearchCriteria;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=none", "spring.sql.init.mode=always"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaSuspendedPaymentRepositoryAdapter.class)
@Testcontainers
class JpaSuspendedPaymentRepositoryAdapterTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaSuspendedPaymentRepositoryAdapter adapter;
    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void clearTable() {
        em.getEntityManager().createNativeQuery("DELETE FROM suspended_payments").executeUpdate();
        em.flush();
        em.clear();
    }

    @Test
    void should_apply_every_filter_with_the_capped_limit() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.suspendedSearch_appliesParameterizedFilters)
        persist(
                suspended("00000000-0000-0000-0000-000000000611", "DE89370400440532013000", "AML_THRESHOLD",
                        "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null),
                suspended("00000000-0000-0000-0000-000000000612", "DE89370400440532013000", "SANCTIONS_SCREENING",
                        "PENDING", Timestamp.valueOf("2026-10-14 09:00:00"), null),
                suspended("00000000-0000-0000-0000-000000000613", "DE89370400440532013000", "AML_THRESHOLD",
                        "RELEASED", Timestamp.valueOf("2026-10-13 08:00:00"), Timestamp.valueOf("2026-10-13 15:00:00")));
        SuspendedPaymentSearchCriteria criteria = SuspendedPaymentSearchCriteria.from("DE89370400440532013000",
                "aml_threshold", "pending", "2026-10-01T00:00:00", "2026-10-31T00:00:00", 5);

        // WHEN
        List<SuspendedPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(PaymentId.of(UUID.fromString("00000000-0000-0000-0000-000000000611")));
        assertThat(results.get(0).holdReason()).contains("AML_THRESHOLD");
        assertThat(results.get(0).heldAt()).contains(LocalDateTime.of(2026, 10, 14, 8, 0));
    }

    @Test
    void should_find_a_payment_by_id() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.suspendedFindById_foundAndMissing, found case)
        persist(suspended("00000000-0000-0000-0000-000000000614", "DE89370400440532013000", "AML_THRESHOLD",
                "RELEASED", Timestamp.valueOf("2026-10-14 08:00:00"), Timestamp.valueOf("2026-10-13 15:45:00")));

        // WHEN
        Optional<SuspendedPayment> found = adapter.findById(PaymentId.parse("00000000-0000-0000-0000-000000000614"));

        // THEN
        assertThat(found).isPresent();
        assertThat(found.get().reviewStatus()).contains("RELEASED");
        assertThat(found.get().releasedAt()).contains(LocalDateTime.of(2026, 10, 13, 15, 45));
    }

    @Test
    void should_find_nothing_for_an_unknown_id() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.suspendedFindById_foundAndMissing, missing case)
        PaymentId unknownId = PaymentId.parse("11111111-1111-1111-1111-111111111111");

        // WHEN
        Optional<SuspendedPayment> found = adapter.findById(unknownId);

        // THEN
        assertThat(found).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] heldFrom={0} heldTo={1} -> empty")
    @CsvSource(value = {
            "2026-10-01, NULL",
            "NULL, 2026-10-31",
            "garbage, NULL",
            "NULL, garbage",
            "2026-10-01 00:00:00, NULL"
    }, nullValues = "NULL")
    void should_return_no_suspended_payment_when_a_held_bound_is_not_in_iso_date_time_format(String heldFrom,
            String heldTo) {
        // GIVEN (remapped from PaymentRepositoryUnitTest, same rows)
        persist(suspended("00000000-0000-0000-0000-000000000651", "DE89370400440532013000", "AML_THRESHOLD",
                "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null));
        SuspendedPaymentSearchCriteria criteria = SuspendedPaymentSearchCriteria.from(null, null, null, heldFrom, heldTo, null);

        // WHEN
        List<SuspendedPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().isEmpty();
    }

    @Test
    void should_apply_inclusive_held_bounds() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.should_apply_inclusive_held_bounds)
        persist(suspended("00000000-0000-0000-0000-000000000652", "DE89370400440532013000", "AML_THRESHOLD",
                "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null));
        SuspendedPaymentSearchCriteria criteria = SuspendedPaymentSearchCriteria.from(null, null, null,
                "2026-10-14T08:00:00", "2026-10-14T08:00:00", null);

        // WHEN
        List<SuspendedPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().extracting(SuspendedPayment::id)
                .containsExactly(PaymentId.parse("00000000-0000-0000-0000-000000000652"));
    }

    @Test
    void should_order_suspended_payments_by_held_at_descending_and_apply_limit() {
        // GIVEN (remapped from PaymentRepositoryUnitTest, same seeds)
        persist(
                suspended("00000000-0000-0000-0000-000000000653", "DE89370400440532013000", "AML_THRESHOLD",
                        "PENDING", Timestamp.valueOf("2026-10-13 08:00:00"), null),
                suspended("00000000-0000-0000-0000-000000000654", "DE89370400440532013000", "AML_THRESHOLD",
                        "PENDING", Timestamp.valueOf("2026-10-15 08:00:00"), null),
                suspended("00000000-0000-0000-0000-000000000655", "DE89370400440532013000", "AML_THRESHOLD",
                        "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null));
        SuspendedPaymentSearchCriteria criteria = SuspendedPaymentSearchCriteria.from(null, null, null, null, null, 2);

        // WHEN
        List<SuspendedPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().extracting(SuspendedPayment::id).containsExactly(
                PaymentId.parse("00000000-0000-0000-0000-000000000654"),
                PaymentId.parse("00000000-0000-0000-0000-000000000655"));
    }

    @Test
    void should_match_suspended_debtor_iban_exactly_and_case_sensitively() {
        // GIVEN (remapped from PaymentRepositoryUnitTest, same seed and filter)
        persist(suspended("00000000-0000-0000-0000-000000000656", "DE89370400440532013000", "AML_THRESHOLD",
                "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null));
        SuspendedPaymentSearchCriteria criteria = SuspendedPaymentSearchCriteria.from("de89370400440532013000",
                null, null, null, null, null);

        // WHEN
        List<SuspendedPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().isEmpty();
    }

    @Test
    void should_replace_every_column_when_saving_an_existing_id() {
        // GIVEN (BR-ING-03 full-replace upsert)
        persist(suspended("00000000-0000-0000-0000-000000000657", "DE89370400440532013000", "AML_THRESHOLD",
                "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null));
        LocalDateTime consumerTime = LocalDateTime.of(2026, 11, 12, 11, 0);
        SuspendedPayment replacement = SuspendedPayment.builder(PaymentId.parse("00000000-0000-0000-0000-000000000657"))
                .money(Money.of(new BigDecimal("1.00"), "USD"))
                .debtorIban("")
                .holdReason("null")
                .reviewStatus("RELEASED")
                .createdAt(consumerTime)
                .updatedAt(consumerTime)
                .build();

        // WHEN
        adapter.save(replacement);
        em.flush();
        em.clear();

        // THEN
        fr.kevin.llps.payments.entity.SuspendedPayment stored =
                em.find(fr.kevin.llps.payments.entity.SuspendedPayment.class, UUID.fromString("00000000-0000-0000-0000-000000000657"));
        assertThat(stored.amount).isEqualByComparingTo("1.00");
        assertThat(stored.currency).isEqualTo("USD");
        assertThat(stored.debtorIban).isEmpty();
        assertThat(stored.debtorName).isNull();
        assertThat(stored.holdReason).isEqualTo("null");
        assertThat(stored.reviewStatus).isEqualTo("RELEASED");
        assertThat(stored.heldAt).isNull();
        assertThat(stored.createdAt).isEqualTo(Timestamp.valueOf("2026-11-12 11:00:00"));
    }

    private void persist(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.flush();
        em.clear();
    }

    private fr.kevin.llps.payments.entity.SuspendedPayment suspended(String id, String debtorIban, String holdReason,
            String reviewStatus, Timestamp heldAt, Timestamp releasedAt) {
        fr.kevin.llps.payments.entity.SuspendedPayment payment = new fr.kevin.llps.payments.entity.SuspendedPayment();
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
