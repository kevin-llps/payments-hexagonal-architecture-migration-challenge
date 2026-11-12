package fr.kevin.llps.payments.adapter.out.persistence.inbound;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.kevin.llps.payments.domain.inbound.InboundPayment;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;

class JpaInboundPaymentMapperTest {

    private final JpaInboundPaymentMapper mapper = new JpaInboundPaymentMapper();

    @Test
    void should_map_every_attribute_to_the_entity() {
        // GIVEN
        InboundPayment payment = InboundPayment.builder(PaymentId.parse("00000000-0000-0000-0000-000000000621"))
                .creditTransferRef("CTR-1")
                .money(Money.of(new BigDecimal("3400.00"), "EUR"))
                .creditorIban("DE89370400440532013000")
                .creditorName("ACME")
                .originBic("BNPAFRPP")
                .valueDate(LocalDate.of(2026, 10, 15))
                .settlementDate(LocalDate.of(2026, 10, 16))
                .status("SETTLED")
                .createdAt(LocalDateTime.of(2026, 10, 15, 7, 45))
                .build();

        // WHEN
        fr.kevin.llps.payments.entity.InboundPayment actual = mapper.toEntity(payment);

        // THEN
        assertThat(actual).isNotNull();
        assertThat(actual.id).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000621"));
        assertThat(actual.valueDate).isEqualTo(Date.valueOf("2026-10-15"));
        assertThat(actual.settlementDate).isEqualTo(Date.valueOf("2026-10-16"));
        assertThat(actual.createdAt).isEqualTo(Timestamp.valueOf("2026-10-15 07:45:00"));
        assertThat(actual.updatedAt).isNull();
    }

    @Test
    void should_map_the_entity_to_the_aggregate() {
        // GIVEN
        fr.kevin.llps.payments.entity.InboundPayment entity = new fr.kevin.llps.payments.entity.InboundPayment();
        entity.id = UUID.fromString("00000000-0000-0000-0000-000000000624");
        entity.creditTransferRef = "CTR-4";
        entity.valueDate = Date.valueOf("2026-10-17");

        // WHEN
        InboundPayment actual = mapper.toDomain(entity);

        // THEN
        assertThat(actual).isNotNull();
        assertThat(actual.creditTransferRef()).contains("CTR-4");
        assertThat(actual.valueDate()).contains(LocalDate.of(2026, 10, 17));
        assertThat(actual.settlementDate()).isEmpty();
    }
}
