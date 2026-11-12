package fr.kevin.llps.payments.adapter.out.persistence.outbound;

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

import fr.kevin.llps.payments.domain.outbound.OutboundPayment;
import fr.kevin.llps.payments.domain.outbound.query.OutboundPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=none", "spring.sql.init.mode=always"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaOutboundPaymentRepositoryAdapter.class)
@Testcontainers
class JpaOutboundPaymentRepositoryAdapterTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaOutboundPaymentRepositoryAdapter adapter;
    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void clearTable() {
        em.getEntityManager().createNativeQuery("DELETE FROM outbound_payments").executeUpdate();
        em.flush();
        em.clear();
    }

    @Test
    void should_apply_every_filter() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.outboundSearch_appliesParameterizedClauses)
        persist(outbound("00000000-0000-0000-0000-000000000631", "OUT-1", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-16")),
                outbound("00000000-0000-0000-0000-000000000632", "OUT-2", "DE89370400440532013000", "BNPAFRPP",
                        "SHA", "SENT", Date.valueOf("2026-10-16")),
                outbound("00000000-0000-0000-0000-000000000633", "OUT-3", "DE89370400440532013000", "ABNANL2A",
                        "OUR", "SENT", Date.valueOf("2026-10-16")));
        OutboundPaymentSearchCriteria criteria = OutboundPaymentSearchCriteria.from("DE89370400440532013000", "abna",
                "sha", "sent", "2026-10-01", "2026-10-31", 20);

        // WHEN
        List<OutboundPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).hasSize(1);
        assertThat(results.get(0).instructionId()).contains("OUT-1");
        assertThat(results.get(0).requestedExecutionDate()).contains(LocalDate.of(2026, 10, 16));
    }

    @Test
    void should_find_a_payment_by_id() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.outboundFindById_foundAndMissing, found case)
        persist(outbound("00000000-0000-0000-0000-000000000634", "OUT-4", "DE89370400440532013000", "ABNANL2A",
                "SHA", "SENT", Date.valueOf("2026-10-18")));

        // WHEN
        Optional<OutboundPayment> found = adapter.findById(PaymentId.parse("00000000-0000-0000-0000-000000000634"));

        // THEN
        assertThat(found).isPresent();
        assertThat(found.get().instructionId()).contains("OUT-4");
    }

    @Test
    void should_find_nothing_for_an_unknown_id() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.outboundFindById_foundAndMissing, missing case)
        PaymentId unknownId = PaymentId.parse("11111111-1111-1111-1111-111111111111");

        // WHEN
        Optional<OutboundPayment> found = adapter.findById(unknownId);

        // THEN
        assertThat(found).isEmpty();
    }

    @Test
    void should_not_match_lower_case_stored_destination_bic() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.should_not_match_lower_case_stored_destination_bic)
        persist(outbound("00000000-0000-0000-0000-000000000671", "OUT-LC", "DE89370400440532013000", "abnanl2a",
                "SHA", "SENT", Date.valueOf("2026-10-16")));
        OutboundPaymentSearchCriteria criteria = OutboundPaymentSearchCriteria.from(null, "abna", null, null, null, null, null);

        // WHEN
        List<OutboundPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().isEmpty();
    }

    @Test
    void should_order_outbound_payments_by_requested_execution_date_descending_and_apply_limit() {
        // GIVEN (remapped from PaymentRepositoryUnitTest, same seeds)
        persist(outbound("00000000-0000-0000-0000-000000000672", "OUT-OLD", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-14")),
                outbound("00000000-0000-0000-0000-000000000673", "OUT-NEW", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-18")),
                outbound("00000000-0000-0000-0000-000000000674", "OUT-MID", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-16")));
        OutboundPaymentSearchCriteria criteria = OutboundPaymentSearchCriteria.from(null, null, null, null, null, null, 2);

        // WHEN
        List<OutboundPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().extracting(p -> p.instructionId().orElse(null)).containsExactly("OUT-NEW", "OUT-MID");
    }

    @Test
    void should_apply_inclusive_outbound_execution_bounds() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.should_apply_inclusive_outbound_execution_bounds)
        persist(outbound("00000000-0000-0000-0000-000000000675", "OUT-EQ", "DE89370400440532013000", "ABNANL2A",
                "SHA", "SENT", Date.valueOf("2026-10-16")));
        OutboundPaymentSearchCriteria criteria = OutboundPaymentSearchCriteria.from(null, null, null, null, "2026-10-16",
                "2026-10-16", null);

        // WHEN
        List<OutboundPayment> results = adapter.search(criteria);

        // THEN
        assertThat(results).isNotNull().extracting(p -> p.instructionId().orElse(null)).containsExactly("OUT-EQ");
    }

    @Test
    void should_replace_every_column_when_saving_an_existing_id() {
        // GIVEN (BR-ING-03 full-replace upsert)
        persist(outbound("00000000-0000-0000-0000-000000000676", "OUT-U", "DE89370400440532013000", "ABNANL2A",
                "SHA", "SENT", Date.valueOf("2026-10-16")));
        LocalDateTime consumerTime = LocalDateTime.of(2026, 11, 12, 11, 0);
        OutboundPayment replacement = OutboundPayment.builder(PaymentId.parse("00000000-0000-0000-0000-000000000676"))
                .instructionId("")
                .money(Money.of(BigDecimal.ZERO, "EUR"))
                .chargesType("xx")
                .destinationBic("abnanl2a")
                .createdAt(consumerTime)
                .updatedAt(consumerTime)
                .build();

        // WHEN
        adapter.save(replacement);
        em.flush();
        em.clear();

        // THEN
        fr.kevin.llps.payments.entity.OutboundPayment stored = em.find(fr.kevin.llps.payments.entity.OutboundPayment.class,
                UUID.fromString("00000000-0000-0000-0000-000000000676"));
        assertThat(stored.instructionId).isEmpty();
        assertThat(stored.chargesType).isEqualTo("xx");
        assertThat(stored.destinationBic).isEqualTo("abnanl2a");
        assertThat(stored.requestedExecutionDate).isNull();
        assertThat(stored.createdAt).isEqualTo(Timestamp.valueOf("2026-11-12 11:00:00"));
    }

    private void persist(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.flush();
        em.clear();
    }

    private fr.kevin.llps.payments.entity.OutboundPayment outbound(String id, String instructionId, String debtorIban,
            String destinationBic, String chargesType, String status, Date requestedExecutionDate) {
        fr.kevin.llps.payments.entity.OutboundPayment payment = new fr.kevin.llps.payments.entity.OutboundPayment();
        payment.id = UUID.fromString(id);
        payment.instructionId = instructionId;
        payment.amount = new BigDecimal("780.00");
        payment.currency = "EUR";
        payment.debtorIban = debtorIban;
        payment.debtorName = "ACME";
        payment.destinationBic = destinationBic;
        payment.requestedExecutionDate = requestedExecutionDate;
        payment.chargesType = chargesType;
        payment.status = status;
        payment.createdAt = Timestamp.valueOf(requestedExecutionDate.toString() + " 08:00:00");
        payment.updatedAt = payment.createdAt;
        return payment;
    }
}
