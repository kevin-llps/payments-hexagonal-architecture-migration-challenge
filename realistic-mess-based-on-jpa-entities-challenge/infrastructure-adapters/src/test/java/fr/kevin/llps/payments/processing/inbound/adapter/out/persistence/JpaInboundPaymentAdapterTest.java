package fr.kevin.llps.payments.processing.inbound.adapter.out.persistence;

import fr.kevin.llps.payments.JpaAdapterTestSupport;
import fr.kevin.llps.payments.processing.inbound.InboundPaymentSearchCriteria;
import fr.kevin.llps.payments.processing.inbound.adapter.out.persistence.entity.InboundPayment;
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
 * {@link JpaInboundPaymentAdapter} against PostgreSQL; carried over from the legacy PaymentRepositoryUnitTest.
 */
@Import(JpaInboundPaymentAdapter.class)
class JpaInboundPaymentAdapterTest extends JpaAdapterTestSupport {

    @Autowired
    private JpaInboundPaymentAdapter inboundRepository;

    // date rendering moved to PaymentControllerWebTest.inboundSearch_formatsValueAndSettlementDates
    @Test
    void inboundSearch_appliesFiltersAndDefaultLimit() {
        persist(
                inbound("00000000-0000-0000-0000-000000000621", "CTR-1", "DE89370400440532013000", "BNPAFRPP",
                        "SETTLED", "EUR", Date.valueOf("2026-10-15"), Date.valueOf("2026-10-15")),
                inbound("00000000-0000-0000-0000-000000000622", "CTR-2", "DE89370400440532013000", "ABNANL2A",
                        "RECEIVED", "EUR", Date.valueOf("2026-10-16"), null),
                inbound("00000000-0000-0000-0000-000000000623", "CTR-3", "DE89370400440532013000", "BNPAFRPP",
                        "SETTLED", "EUR", Date.valueOf("2026-09-01"), Date.valueOf("2026-09-01")));

        var results = inboundRepository.search(InboundPaymentSearchCriteria.from("DE89370400440532013000", "bnpa",
                "settled", "eur", "2026-10-01", "2026-10-31", null));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).creditTransferRef()).isEqualTo("CTR-1");
        assertThat(results.get(0).valueDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(results.get(0).settlementDate()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void inboundSearch_creditorIbanIsExact_originBicMatchesStoredValueIgnoringCase() {
        persist(inbound("00000000-0000-0000-0000-000000000625", "CTR-5", "DE89370400440532013000", "bnpafrpp",
                "RECEIVED", "EUR", Date.valueOf("2026-10-15"), null));

        assertThat(inboundRepository.search(inboundCriteria(null, "NPAF", null)))
                .extracting(r -> r.creditTransferRef()).containsExactly("CTR-5");
        assertThat(inboundRepository.search(inboundCriteria("DE89", null, null))).isEmpty();
    }

    @Test
    void inboundSearch_ordersByValueDateThenCreatedAtDescending() {
        InboundPayment older = inbound("00000000-0000-0000-0000-000000000626", "CTR-OLD", "DE89370400440532013000",
                "BNPAFRPP", "RECEIVED", "EUR", Date.valueOf("2026-10-14"), null);
        InboundPayment sameDayEarly = inbound("00000000-0000-0000-0000-000000000627", "CTR-EARLY",
                "DE89370400440532013000", "BNPAFRPP", "RECEIVED", "EUR", Date.valueOf("2026-10-16"), null);
        InboundPayment sameDayLate = inbound("00000000-0000-0000-0000-000000000628", "CTR-LATE",
                "DE89370400440532013000", "BNPAFRPP", "RECEIVED", "EUR", Date.valueOf("2026-10-16"), null);
        sameDayLate.createdAt = Timestamp.valueOf("2026-10-16 18:00:00");
        persist(older, sameDayEarly, sameDayLate);

        assertThat(inboundRepository.search(inboundCriteria(null, null, null))).extracting(r -> r.creditTransferRef())
                .containsExactly("CTR-LATE", "CTR-EARLY", "CTR-OLD");
    }

    @Test
    void inboundSearch_limitAbove200_isNotCapped() {
        for (int i = 0; i < 201; i++) {
            persist(inbound(String.format("00000000-0000-0000-0000-00000002%04d", i), "CTR-L" + i,
                    "DE89370400440532013000", "BNPAFRPP", "RECEIVED", "EUR", Date.valueOf("2026-10-15"), null));
        }

        assertThat(inboundRepository.search(inboundCriteria(null, null, 500))).hasSize(201);
        assertThat(inboundRepository.search(inboundCriteria(null, null, null))).hasSize(50);
    }

    // date rendering moved to PaymentControllerWebTest.inboundDetails_formatsDates
    @Test
    void inboundFindById_foundAndMissing() {
        persist(inbound("00000000-0000-0000-0000-000000000624", "CTR-4", "DE89370400440532013000", "BNPAFRPP",
                "RECEIVED", "EUR", Date.valueOf("2026-10-17"), null));

        var found = inboundRepository.findById(PaymentId.parse("00000000-0000-0000-0000-000000000624")).orElseThrow();
        assertThat(found.creditTransferRef()).isEqualTo("CTR-4");
        assertThat(found.valueDate()).isEqualTo(LocalDate.of(2026, 10, 17));
        assertThat(found.settlementDate()).isNull();
        assertThat(inboundRepository.findById(PaymentId.parse("11111111-1111-1111-1111-111111111111"))).isEmpty();
    }

    private InboundPaymentSearchCriteria inboundCriteria(String creditorIban, String originBic, Integer limit) {
        return InboundPaymentSearchCriteria.from(creditorIban, originBic, null, null, null, null, limit);
    }

    private InboundPayment inbound(String id, String creditTransferRef, String creditorIban, String originBic,
            String status, String currency, Date valueDate, Date settlementDate) {
        InboundPayment payment = new InboundPayment();
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
