package fr.kevin.llps.payments.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
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

import fr.kevin.llps.payments.dto.OutboundPaymentSearchRequest;
import fr.kevin.llps.payments.entity.InstantPayment;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.entity.SuspendedPayment;

import jakarta.persistence.PersistenceException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OutboundPaymentRepository.class)
@Testcontainers
class PaymentRepositoryUnitTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private OutboundPaymentRepository outboundRepository;
    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void clearTables() {
        em.getEntityManager().createNativeQuery("DELETE FROM instant_payments").executeUpdate();
        em.getEntityManager().createNativeQuery("DELETE FROM suspended_payments").executeUpdate();
        em.getEntityManager().createNativeQuery("DELETE FROM inbound_payments").executeUpdate();
        em.getEntityManager().createNativeQuery("DELETE FROM outbound_payments").executeUpdate();
        em.getEntityManager().createNativeQuery("DELETE FROM recalls").executeUpdate();
        em.flush();
        em.clear();
    }

    // ---------------- outbound ----------------

    @Test
    void outboundSearch_appliesParameterizedClauses() {
        persist(
                outbound("00000000-0000-0000-0000-000000000631", "OUT-1", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-16")),
                outbound("00000000-0000-0000-0000-000000000632", "OUT-2", "DE89370400440532013000", "BNPAFRPP",
                        "SHA", "SENT", Date.valueOf("2026-10-16")),
                outbound("00000000-0000-0000-0000-000000000633", "OUT-3", "DE89370400440532013000", "ABNANL2A",
                        "OUR", "SENT", Date.valueOf("2026-10-16")));

        OutboundPaymentSearchRequest request = new OutboundPaymentSearchRequest();
        request.debtorIban = "DE89370400440532013000";
        request.destinationBic = "abna";
        request.chargesType = "sha";
        request.status = "sent";
        request.executionFrom = "2026-10-01";
        request.executionTo = "2026-10-31";
        request.limit = 20;

        List<OutboundPayment> results = outboundRepository.search(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).instructionId).isEqualTo("OUT-1");
        assertThat(results.get(0).requestedExecutionDate).isEqualTo(Date.valueOf("2026-10-16"));
    }

    @Test
    void outboundFindById_foundAndMissing() {
        persist(outbound("00000000-0000-0000-0000-000000000634", "OUT-4", "DE89370400440532013000", "ABNANL2A",
                "SHA", "SENT", Date.valueOf("2026-10-18")));

        OutboundPayment found = outboundRepository.findById("00000000-0000-0000-0000-000000000634");
        assertThat(found.instructionId).isEqualTo("OUT-4");
        assertThat(outboundRepository.findById("11111111-1111-1111-1111-111111111111")).isNull();
    }

    // ---------------- characterization: Outbound filters, order, limit and dates (BR-OUT-02, BR-OUT-04, BR-OUT-05) ----------------

    @Test
    void should_not_match_lower_case_stored_destination_bic() {
        // GIVEN
        persist(outbound("00000000-0000-0000-0000-000000000671", "OUT-LC", "DE89370400440532013000", "abnanl2a",
                "SHA", "SENT", Date.valueOf("2026-10-16")));
        OutboundPaymentSearchRequest request = new OutboundPaymentSearchRequest();
        request.destinationBic = "abna";

        // WHEN
        List<OutboundPayment> results = outboundRepository.search(request);

        // THEN
        assertThat(results).isEmpty();
    }

    @Test
    void should_order_outbound_payments_by_requested_execution_date_descending_and_apply_limit() {
        // GIVEN
        persist(
                outbound("00000000-0000-0000-0000-000000000672", "OUT-OLD", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-14")),
                outbound("00000000-0000-0000-0000-000000000673", "OUT-NEW", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-18")),
                outbound("00000000-0000-0000-0000-000000000674", "OUT-MID", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-16")));
        OutboundPaymentSearchRequest request = new OutboundPaymentSearchRequest();
        request.limit = 2;

        // WHEN
        List<OutboundPayment> results = outboundRepository.search(request);

        // THEN
        assertThat(results).extracting(p -> p.instructionId).containsExactly("OUT-NEW", "OUT-MID");
    }

    @ParameterizedTest(name = "[{index}] executionFrom={0} executionTo={1}")
    @CsvSource(value = {
            "garbage, NULL",
            "NULL, 2026-02-30x",
            "2026-10-16T00:00:00, NULL"
    }, nullValues = "NULL")
    void should_reject_outbound_execution_bound_not_in_strict_iso_date_format_with_message_less_error(String from,
            String to) {
        // GIVEN
        OutboundPaymentSearchRequest request = new OutboundPaymentSearchRequest();
        request.executionFrom = from;
        request.executionTo = to;

        // WHEN / THEN
        assertThatThrownBy(() -> outboundRepository.search(request))
                .satisfies(e -> assertThat(rootCauseOf(e)).isExactlyInstanceOf(IllegalArgumentException.class)
                        .hasMessage(null));
    }

    @Test
    void should_apply_inclusive_outbound_execution_bounds() {
        // GIVEN
        persist(outbound("00000000-0000-0000-0000-000000000675", "OUT-EQ", "DE89370400440532013000", "ABNANL2A",
                "SHA", "SENT", Date.valueOf("2026-10-16")));
        OutboundPaymentSearchRequest request = new OutboundPaymentSearchRequest();
        request.executionFrom = "2026-10-16";
        request.executionTo = "2026-10-16";

        // WHEN
        List<OutboundPayment> results = outboundRepository.search(request);

        // THEN
        assertThat(results).extracting(p -> p.instructionId).containsExactly("OUT-EQ");
    }

    // ---------------- characterization: schema constraints (BR-SHR-09, BR-DAT-01) ----------------

    @Test
    void should_reject_instant_payment_without_currency() {
        // GIVEN
        InstantPayment payment = instant("00000000-0000-0000-0000-000000000682", "E2E-R2", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), null);

        // WHEN / THEN
        assertThatThrownBy(() -> persist(payment))
                .isInstanceOf(PersistenceException.class)
                .rootCause().hasMessageContaining("not-null");
    }

    @Test
    void should_reject_outbound_payment_without_amount() {
        // GIVEN
        OutboundPayment payment = outbound("00000000-0000-0000-0000-000000000683", "OUT-NA", "DE89370400440532013000",
                "ABNANL2A", "SHA", "SENT", Date.valueOf("2026-10-16"));
        payment.amount = null;

        // WHEN / THEN
        assertThatThrownBy(() -> persist(payment))
                .isInstanceOf(PersistenceException.class)
                .rootCause().hasMessageContaining("not-null");
    }

    @Test
    void should_reject_currency_longer_than_three_characters() {
        // GIVEN
        SuspendedPayment payment = suspended("00000000-0000-0000-0000-000000000684", "DE89370400440532013000",
                "AML_THRESHOLD", "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null);
        payment.currency = "EURO";

        // WHEN / THEN
        assertThatThrownBy(() -> persist(payment))
                .isInstanceOf(PersistenceException.class)
                .rootCause().hasMessageContaining("value too long");
    }

    // ---------------- helpers ----------------

    private static Throwable rootCauseOf(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private void persist(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.flush();
        em.clear();
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

    private OutboundPayment outbound(String id, String instructionId, String debtorIban, String destinationBic,
            String chargesType, String status, Date requestedExecutionDate) {
        OutboundPayment payment = new OutboundPayment();
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
