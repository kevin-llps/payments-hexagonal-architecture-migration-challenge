package fr.kevin.llps.payments.adapter.out.persistence.instant;

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

import fr.kevin.llps.payments.domain.instant.InstantPayment;
import fr.kevin.llps.payments.domain.instant.query.InstantPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=none", "spring.sql.init.mode=always"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaInstantPaymentRepositoryAdapter.class)
@Testcontainers
class JpaInstantPaymentRepositoryAdapterTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaInstantPaymentRepositoryAdapter adapter;
    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void clearTable() {
        em.getEntityManager().createNativeQuery("DELETE FROM instant_payments").executeUpdate();
        em.flush();
        em.clear();
    }

    @Test
    void should_apply_every_filter() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.instantSearch_appliesAllFilters_andFormatsExecutionDatetime; rendering moved
        // to the REST test). The legacy IBAN fragments "de89" / "fr76" bypassed the service validation; through the criteria
        // factory a filter must have a valid length, so the full IBANs in lower case are used (still case-insensitive contains).
        persist(instant("00000000-0000-0000-0000-000000000601", "E2E-T1", "COMPLETED", "DE89370400440532013000",
                        "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"),
                instant("00000000-0000-0000-0000-000000000602", "E2E-T2", "PENDING", "DE89370400440532013000",
                        "FR7630006000011234567890189", Timestamp.valueOf("2026-10-16 10:00:00"), "EUR"));
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from("E2E-T1", "de89370400440532013000", "fr7630006000011234567890189", "completed",
                "eur", "2026-10-01T00:00:00", "2026-10-31", 10);

        // WHEN
        List<InstantPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).hasSize(1);
        InstantPayment payment = results.get(0);
        assertThat(payment.id()).isEqualTo(PaymentId.parse("00000000-0000-0000-0000-000000000601"));
        assertThat(payment.endToEndId()).contains("E2E-T1");
        assertThat(payment.money().amount()).hasValueSatisfying(amount -> assertThat(amount).isEqualByComparingTo("1500.00"));
        assertThat(payment.executionDatetime()).contains(LocalDateTime.of(2026, 10, 15, 9, 15));
        assertThat(payment.scheme()).contains("SCT INST");
    }

    @Test
    void should_order_instant_payments_by_execution_datetime_descending() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.instantSearch_ordersByExecutionDatetimeDescending)
        persist(instant("00000000-0000-0000-0000-000000000603", "E2E-OLD", "PENDING", "DE89370400440532013000",
                        "FR7630006000011234567890189", Timestamp.valueOf("2026-10-10 08:00:00"), "EUR"),
                instant("00000000-0000-0000-0000-000000000604", "E2E-NEW", "COMPLETED", "DE89370400440532013000",
                        "FR7630006000011234567890189", Timestamp.valueOf("2026-10-16 08:00:00"), "EUR"));
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from(null, null, null, null, null, null, null, null);

        // WHEN
        List<InstantPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().extracting(p -> p.endToEndId().orElse(null)).containsExactly("E2E-NEW", "E2E-OLD");
    }

    @Test
    void should_find_a_payment_by_id() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.instantFindById_foundAndMissing, found case)
        persist(instant("00000000-0000-0000-0000-000000000605", "E2E-F1", "COMPLETED", "DE89370400440532013000",
                "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));

        // WHEN
        Optional<InstantPayment> found = adapter.findById(PaymentId.parse("00000000-0000-0000-0000-000000000605"));

        // THEN
        assertThat(found).isPresent();
        assertThat(found.get().endToEndId()).contains("E2E-F1");
        assertThat(found.get().executionDatetime()).contains(LocalDateTime.of(2026, 10, 15, 9, 15));
    }

    @Test
    void should_find_nothing_for_an_unknown_id() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.instantFindById_foundAndMissing, missing case)
        PaymentId unknownId = PaymentId.parse("11111111-1111-1111-1111-111111111111");

        // WHEN
        Optional<InstantPayment> found = adapter.findById(unknownId);

        // THEN
        assertThat(found).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] executionFrom={0} executionTo={1} -> {2} rows")
    @CsvSource(value = {
            "garbage, NULL, 2",
            "NULL, garbage, 2",
            "2026-10-16, NULL, 1",
            "NULL, 2026-10-15, 1",
            "2026-10-15T09:15:00, 2026-10-15T09:15:00, 1"
    }, nullValues = "NULL")
    void should_apply_inclusive_instant_execution_bounds_and_ignore_unparsable_ones(String from, String to,
            int expectedCount) {
        // GIVEN (remapped from PaymentRepositoryUnitTest, same rows)
        persist(instant("00000000-0000-0000-0000-000000000641", "E2E-B1", "COMPLETED", "DE89370400440532013000",
                        "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"),
                instant("00000000-0000-0000-0000-000000000642", "E2E-B2", "COMPLETED", "DE89370400440532013000",
                        "FR7630006000011234567890189", Timestamp.valueOf("2026-10-16 00:00:00"), "EUR"));
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from(null, null, null, null, null, from, to, null);

        // WHEN
        List<InstantPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).hasSize(expectedCount);
    }

    @Test
    void should_match_instant_iban_filter_case_insensitively_as_contains() {
        // GIVEN (remapped from PaymentRepositoryUnitTest; the legacy fragment " 0532013 " is shorter than a valid IBAN filter,
        // so a 15-character inner fragment with surrounding spaces is used: still trimmed, contains, case-insensitive)
        persist(instant("00000000-0000-0000-0000-000000000643", "E2E-C1", "COMPLETED", "DE89370400440532013000",
                "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from(null, " 370400440532013 ", null, null, null,
                null, null, null);

        // WHEN
        List<InstantPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().extracting(p -> p.endToEndId().orElse(null)).containsExactly("E2E-C1");
    }

    @Test
    void should_not_strip_inner_spaces_from_instant_iban_filter() {
        // GIVEN (remapped from PaymentRepositoryUnitTest, same spaced IBAN)
        persist(instant("00000000-0000-0000-0000-000000000644", "E2E-C2", "COMPLETED", "DE89370400440532013000",
                "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from(null, "DE89 3704 0044 0532 0130 00",
                null, null, null, null, null, null);

        // WHEN
        List<InstantPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().isEmpty();
    }

    @Test
    void should_match_end_to_end_id_exactly_and_case_sensitively() {
        // GIVEN (remapped from PaymentRepositoryUnitTest, same value)
        persist(instant("00000000-0000-0000-0000-000000000645", "E2E-Case", "COMPLETED", "DE89370400440532013000",
                "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from("e2e-case", null, null, null, null,
                null, null, null);

        // WHEN
        List<InstantPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().isEmpty();
    }

    @Test
    void should_return_no_instant_payment_when_limit_is_zero() {
        // GIVEN (remapped from PaymentRepositoryUnitTest, same limit 0)
        persist(instant("00000000-0000-0000-0000-000000000646", "E2E-L0", "COMPLETED", "DE89370400440532013000",
                "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from(null, null, null, null, null, null, null, 0);

        // WHEN
        List<InstantPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().isEmpty();
    }

    @Test
    void should_round_amount_to_two_fractional_digits() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.should_round_amount_to_two_fractional_digits)
        fr.kevin.llps.payments.entity.InstantPayment payment = instant("00000000-0000-0000-0000-000000000681", "E2E-R1",
                "COMPLETED", "DE89370400440532013000", "FR7630006000011234567890189",
                Timestamp.valueOf("2026-10-15 09:15:00"), "EUR");
        payment.amount = new BigDecimal("10.005");
        persist(payment);

        // WHEN
        Optional<InstantPayment> found = adapter.findById(PaymentId.parse("00000000-0000-0000-0000-000000000681"));

        // THEN
        assertThat(found).isPresent();
        assertThat(found.get().money().amount()).contains(new BigDecimal("10.01"));
    }

    @Test
    void should_replace_every_column_when_saving_an_existing_id() {
        // GIVEN (BR-ING-03 full-replace upsert)
        persist(instant("00000000-0000-0000-0000-000000000647", "E2E-U1", "COMPLETED", "DE89370400440532013000",
                "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));
        LocalDateTime consumerTime = LocalDateTime.of(2026, 11, 12, 11, 0);
        InstantPayment replacement = InstantPayment.builder(PaymentId.parse("00000000-0000-0000-0000-000000000647"))
                .endToEndId("")
                .money(Money.of(BigDecimal.ZERO, "EUR"))
                .status("null")
                .scheme("SCT")
                .createdAt(consumerTime)
                .updatedAt(consumerTime)
                .build();

        // WHEN
        adapter.save(replacement);
        em.flush();
        em.clear();

        // THEN
        fr.kevin.llps.payments.entity.InstantPayment stored = em.find(fr.kevin.llps.payments.entity.InstantPayment.class,
                UUID.fromString("00000000-0000-0000-0000-000000000647"));
        assertThat(stored.endToEndId).isEmpty();
        assertThat(stored.status).isEqualTo("null");
        assertThat(stored.scheme).isEqualTo("SCT");
        assertThat(stored.debtorIban).isNull();
        assertThat(stored.executionDatetime).isNull();
        assertThat(stored.createdAt).isEqualTo(Timestamp.valueOf("2026-11-12 11:00:00"));
    }

    private void persist(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.flush();
        em.clear();
    }

    private fr.kevin.llps.payments.entity.InstantPayment instant(String id, String endToEndId, String status,
            String debtorIban, String creditorIban, Timestamp executionDatetime, String currency) {
        fr.kevin.llps.payments.entity.InstantPayment payment = new fr.kevin.llps.payments.entity.InstantPayment();
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
