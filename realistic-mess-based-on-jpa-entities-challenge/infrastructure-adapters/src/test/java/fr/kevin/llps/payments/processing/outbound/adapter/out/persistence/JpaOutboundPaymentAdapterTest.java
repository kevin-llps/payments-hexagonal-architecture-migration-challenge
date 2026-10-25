package fr.kevin.llps.payments.processing.outbound.adapter.out.persistence;

import fr.kevin.llps.payments.JpaAdapterTestSupport;
import fr.kevin.llps.payments.processing.outbound.OutboundPaymentSearchCriteria;
import fr.kevin.llps.payments.processing.outbound.adapter.out.persistence.entity.OutboundPayment;
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
 * {@link JpaOutboundPaymentAdapter} against PostgreSQL; carried over from the legacy PaymentRepositoryUnitTest.
 */
@Import(JpaOutboundPaymentAdapter.class)
class JpaOutboundPaymentAdapterTest extends JpaAdapterTestSupport {

    @Autowired
    private JpaOutboundPaymentAdapter outboundRepository;

    @Test
    void outboundSearch_appliesParameterizedClauses() {
        persist(
                outbound("00000000-0000-0000-0000-000000000631", "OUT-1", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-16")),
                outbound("00000000-0000-0000-0000-000000000632", "OUT-2", "DE89370400440532013000", "BNPAFRPP",
                        "SHA", "SENT", Date.valueOf("2026-10-16")),
                outbound("00000000-0000-0000-0000-000000000633", "OUT-3", "DE89370400440532013000", "ABNANL2A",
                        "OUR", "SENT", Date.valueOf("2026-10-16")));

        var results = outboundRepository.search(OutboundPaymentSearchCriteria.from("DE89370400440532013000", "abna",
                "sha", "sent", "2026-10-01", "2026-10-31", 20));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).instructionId()).isEqualTo("OUT-1");
        assertThat(results.get(0).requestedExecutionDate()).isEqualTo(LocalDate.of(2026, 10, 16));
    }

    @Test
    void outboundSearch_debtorIbanIsExact_destinationBicDoesNotMatchLowercaseStoredValue() {
        persist(
                outbound("00000000-0000-0000-0000-000000000635", "OUT-UPPER", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-16")),
                outbound("00000000-0000-0000-0000-000000000636", "OUT-LOWER", "DE89370400440532013000", "abnanl2a",
                        "SHA", "SENT", Date.valueOf("2026-10-16")));

        assertThat(outboundRepository.search(outboundCriteria(null, "abna", null)))
                .extracting(p -> p.instructionId()).containsExactly("OUT-UPPER");
        assertThat(outboundRepository.search(outboundCriteria("DE89", null, null))).isEmpty();
    }

    @Test
    void outboundSearch_ordersByRequestedExecutionDateDescending() {
        persist(
                outbound("00000000-0000-0000-0000-000000000637", "OUT-EARLY", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-10")),
                outbound("00000000-0000-0000-0000-000000000638", "OUT-LATE", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-20")));

        assertThat(outboundRepository.search(outboundCriteria(null, null, null))).extracting(p -> p.instructionId())
                .containsExactly("OUT-LATE", "OUT-EARLY");
    }

    @Test
    void outboundSearch_limitAbove200_isCappedAt200() {
        for (int i = 0; i < 201; i++) {
            persist(outbound(String.format("00000000-0000-0000-0000-00000003%04d", i), "OUT-L" + i,
                    "DE89370400440532013000", "ABNANL2A", "SHA", "SENT", Date.valueOf("2026-10-16")));
        }

        assertThat(outboundRepository.search(outboundCriteria(null, null, 500))).hasSize(200);
    }

    @Test
    void outboundFindById_foundAndMissing() {
        persist(outbound("00000000-0000-0000-0000-000000000634", "OUT-4", "DE89370400440532013000", "ABNANL2A",
                "SHA", "SENT", Date.valueOf("2026-10-18")));

        var found = outboundRepository.findById(PaymentId.parse("00000000-0000-0000-0000-000000000634")).orElseThrow();
        assertThat(found.instructionId()).isEqualTo("OUT-4");
        assertThat(outboundRepository.findById(PaymentId.parse("11111111-1111-1111-1111-111111111111"))).isEmpty();
    }

    private OutboundPaymentSearchCriteria outboundCriteria(String debtorIban, String destinationBic, Integer limit) {
        return OutboundPaymentSearchCriteria.from(debtorIban, destinationBic, null, null, null, null, limit);
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
