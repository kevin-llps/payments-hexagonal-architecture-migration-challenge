package fr.kevin.llps.payments.adapter.out.persistence;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import fr.kevin.llps.payments.entity.InstantPayment;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.entity.SuspendedPayment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Schema constraints of the unchanged {@code schema.sql} (BR-SHR-09, BR-DAT-01), moved unchanged from the legacy
 * {@code PaymentRepositoryUnitTest} at the IT-06 gate.
 */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=none", "spring.sql.init.mode=always"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class JpaSchemaConstraintsTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

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
