package fr.kevin.llps.payments.adapter.out.persistence.inbound;

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

import fr.kevin.llps.payments.domain.inbound.InboundPayment;
import fr.kevin.llps.payments.domain.inbound.query.InboundPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=none", "spring.sql.init.mode=always"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaInboundPaymentRepositoryAdapter.class)
@Testcontainers
class JpaInboundPaymentRepositoryAdapterTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaInboundPaymentRepositoryAdapter adapter;
    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void clearTable() {
        em.getEntityManager().createNativeQuery("DELETE FROM inbound_payments").executeUpdate();
        em.flush();
        em.clear();
    }

    @Test
    void should_apply_every_filter_with_the_default_limit() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.inboundSearch_appliesFiltersAndDefaultLimit; date rendering moved to the REST test)
        persist(inbound("00000000-0000-0000-0000-000000000621", "CTR-1", "DE89370400440532013000", "BNPAFRPP",
                        "SETTLED", "EUR", Date.valueOf("2026-10-15"), Date.valueOf("2026-10-15")),
                inbound("00000000-0000-0000-0000-000000000622", "CTR-2", "DE89370400440532013000", "ABNANL2A",
                        "RECEIVED", "EUR", Date.valueOf("2026-10-16"), null),
                inbound("00000000-0000-0000-0000-000000000623", "CTR-3", "DE89370400440532013000", "BNPAFRPP",
                        "SETTLED", "EUR", Date.valueOf("2026-09-01"), Date.valueOf("2026-09-01")));
        InboundPaymentSearchCriteria criteria = InboundPaymentSearchCriteria.from("DE89370400440532013000", "bnpa",
                "settled", "eur", "2026-10-01", "2026-10-31", null);

        // WHEN
        List<InboundPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).hasSize(1);
        assertThat(results.get(0).creditTransferRef()).contains("CTR-1");
        assertThat(results.get(0).valueDate()).contains(LocalDate.of(2026, 10, 15));
        assertThat(results.get(0).settlementDate()).contains(LocalDate.of(2026, 10, 15));
    }

    @Test
    void should_find_a_payment_by_id() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.inboundFindById_foundAndMissing, found case)
        persist(inbound("00000000-0000-0000-0000-000000000624", "CTR-4", "DE89370400440532013000", "BNPAFRPP",
                "RECEIVED", "EUR", Date.valueOf("2026-10-17"), null));

        // WHEN
        Optional<InboundPayment> found = adapter.findById(PaymentId.parse("00000000-0000-0000-0000-000000000624"));

        // THEN
        assertThat(found).isPresent();
        assertThat(found.get().creditTransferRef()).contains("CTR-4");
        assertThat(found.get().valueDate()).contains(LocalDate.of(2026, 10, 17));
        assertThat(found.get().settlementDate()).isEmpty();
    }

    @Test
    void should_find_nothing_for_an_unknown_id() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.inboundFindById_foundAndMissing, missing case)
        PaymentId unknownId = PaymentId.parse("11111111-1111-1111-1111-111111111111");

        // WHEN
        Optional<InboundPayment> found = adapter.findById(unknownId);

        // THEN
        assertThat(found).isEmpty();
    }

    @Test
    void should_order_inbound_payments_by_value_date_then_created_at_descending() {
        // GIVEN (remapped from PaymentRepositoryUnitTest, same seeds)
        fr.kevin.llps.payments.entity.InboundPayment olderSameDay = inbound("00000000-0000-0000-0000-000000000661",
                "CTR-A", "DE89370400440532013000", "BNPAFRPP", "SETTLED", "EUR", Date.valueOf("2026-10-15"), null);
        fr.kevin.llps.payments.entity.InboundPayment newerSameDay = inbound("00000000-0000-0000-0000-000000000662",
                "CTR-B", "DE89370400440532013000", "BNPAFRPP", "SETTLED", "EUR", Date.valueOf("2026-10-15"), null);
        newerSameDay.createdAt = Timestamp.valueOf("2026-10-15 18:00:00");
        fr.kevin.llps.payments.entity.InboundPayment latestValueDate = inbound("00000000-0000-0000-0000-000000000663",
                "CTR-C", "DE89370400440532013000", "BNPAFRPP", "SETTLED", "EUR", Date.valueOf("2026-10-16"), null);
        persist(olderSameDay, newerSameDay, latestValueDate);
        InboundPaymentSearchCriteria criteria = InboundPaymentSearchCriteria.from(null, null, null, null, null, null, null);

        // WHEN
        List<InboundPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().extracting(p -> p.creditTransferRef().orElse(null))
                .containsExactly("CTR-C", "CTR-B", "CTR-A");
    }

    @ParameterizedTest(name = "[{index}] limit {0} -> {1} rows")
    @CsvSource(value = {
            "NULL, 50",
            "201, 201",
            "0, 0"
    }, nullValues = "NULL")
    void should_default_inbound_limit_to_50_without_capping_at_200(Integer limit, int expectedCount) {
        // GIVEN (remapped from PaymentRepositoryUnitTest, same rows)
        for (int i = 0; i < 201; i++) {
            em.persist(inbound(String.format("00000000-0000-0000-0000-0000000%05d", 10000 + i), "CTR-" + i,
                    "DE89370400440532013000", "BNPAFRPP", "SETTLED", "EUR", Date.valueOf("2026-10-15"), null));
        }
        em.flush();
        em.clear();
        InboundPaymentSearchCriteria criteria = InboundPaymentSearchCriteria.from(null, null, null, null, null, null, limit);

        // WHEN
        List<InboundPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).hasSize(expectedCount);
    }

    @Test
    void should_apply_inclusive_inbound_value_date_bounds() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.should_apply_inclusive_inbound_value_date_bounds)
        persist(inbound("00000000-0000-0000-0000-000000000664", "CTR-D", "DE89370400440532013000", "BNPAFRPP",
                "SETTLED", "EUR", Date.valueOf("2026-10-15"), null));
        InboundPaymentSearchCriteria criteria = InboundPaymentSearchCriteria.from(null, null, null, null, "2026-10-15",
                " 2026-10-15 ", null);

        // WHEN
        List<InboundPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().extracting(p -> p.creditTransferRef().orElse(null)).containsExactly("CTR-D");
    }

    @Test
    void should_replace_every_column_when_saving_an_existing_id() {
        // GIVEN (BR-ING-03 full-replace upsert)
        persist(inbound("00000000-0000-0000-0000-000000000665", "CTR-U", "DE89370400440532013000", "BNPAFRPP",
                "SETTLED", "EUR", Date.valueOf("2026-10-15"), Date.valueOf("2026-10-15")));
        LocalDateTime consumerTime = LocalDateTime.of(2026, 11, 12, 11, 0);
        InboundPayment replacement = InboundPayment.builder(PaymentId.parse("00000000-0000-0000-0000-000000000665"))
                .creditTransferRef("")
                .money(Money.of(BigDecimal.ZERO, "EUR"))
                .originBic("null")
                .createdAt(consumerTime)
                .updatedAt(consumerTime)
                .build();

        // WHEN
        adapter.save(replacement);
        em.flush();
        em.clear();

        // THEN
        fr.kevin.llps.payments.entity.InboundPayment stored = em.find(fr.kevin.llps.payments.entity.InboundPayment.class,
                UUID.fromString("00000000-0000-0000-0000-000000000665"));
        assertThat(stored.creditTransferRef).isEmpty();
        assertThat(stored.originBic).isEqualTo("null");
        assertThat(stored.valueDate).isNull();
        assertThat(stored.settlementDate).isNull();
        assertThat(stored.createdAt).isEqualTo(Timestamp.valueOf("2026-11-12 11:00:00"));
    }

    private void persist(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.flush();
        em.clear();
    }

    private fr.kevin.llps.payments.entity.InboundPayment inbound(String id, String creditTransferRef, String creditorIban,
            String originBic, String status, String currency, Date valueDate, Date settlementDate) {
        fr.kevin.llps.payments.entity.InboundPayment payment = new fr.kevin.llps.payments.entity.InboundPayment();
        payment.id = UUID.fromString(id);
        payment.creditTransferRef = creditTransferRef;
        payment.amount = new BigDecimal("3400.00");
        payment.currency = currency;
        payment.creditorIban = creditorIban;
        payment.creditorName = "ACME";
        payment.originBic = originBic;
        payment.valueDate = valueDate;
        payment.settlementDate = settlementDate;
        payment.status = status;
        payment.createdAt = Timestamp.valueOf(valueDate.toString() + " 07:45:00");
        payment.updatedAt = payment.createdAt;
        return payment;
    }
}
